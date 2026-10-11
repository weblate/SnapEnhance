/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.findConstString
import me.rhunk.snapenhance.mapper.ext.findSetViewIds
import me.rhunk.snapenhance.mapper.ext.getClassName
import me.rhunk.snapenhance.mapper.ext.referencedFields
import me.rhunk.snapenhance.mapper.ext.referencedMethods

class NavigationBarMapper : AbstractClassMapper("NavigationBar") {
    val subscriberClass = classReference("subscriberClass")
    val subscriberControllerField = string("subscriberControllerField")
    val navigationCallbacks = map("navigationCallbacks")
    val controllerClass = classReference("controllerClass")
    val navigationManagerField = string("navigationManagerField")
    val navigationManagerClass = classReference("navigationManagerClass")
    val currentPageMethod = string("currentPageMethod")
    val pageType = string("pageType")
    val pageSpecMapField = string("pageSpecMapField")
    val pageSpecClass = classReference("pageSpecClass")
    val pageSpecPageField = string("pageSpecPageField")
    val pageSpecViewIdField = string("pageSpecViewIdField")
    val navigationContainer = string("navigationContainer")
    val navigationTitleCallbackClass = classReference("navigationTitleCallbackClass")
    val navigationTitleBranchField = string("navigationTitleBranchField")
    val navigationTitleViewField = string("navigationTitleViewField")
    val navigationTitleCallbackMethod = string("navigationTitleCallbackMethod")

    init {
        mapper {
            for (subscriber in classes) {
                if (subscriber.methods.none { it.implementation?.findConstString("NgsNavigationBarControllerSubscriber") == true }) continue

                val controllerField = subscriber.fields.singleOrNull { it.type == "Ljava/lang/Object;" } ?: continue

                val callbacks = subscriber.methods.filter {
                    it.name != "<init>" && it.name != "<clinit>" && it.returnType == "V" && it.parameterTypes.size == 1 && it.parameterTypes[0].startsWith("L")
                }.groupBy { it.parameterTypes[0] }.values.maxByOrNull { it.size }?.takeIf { it.size >= 2 } ?: continue

                val controller = callbacks.flatMap { callback ->
                    callback.referencedFields().map { it.definingClass }
                }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.mapNotNull { (type, _) ->
                    getClass(type)
                }.firstOrNull { clazz ->
                    val maps = clazz.fields.count {
                        it.type == "Ljava/util/LinkedHashMap;" || it.type == "Ljava/util/Map;"
                    }
                    (maps >= 2 && clazz.fields.any { it.type.contains("PublishSubject") }) || maps >= 3
                } ?: continue

                val pageSpecClassDef = controller.methods.flatMap { method ->
                    method.referencedFields().flatMap { listOf(it.definingClass, it.type) }
                }.mapNotNull { getClass(it) }.firstOrNull { clazz ->
                    clazz.fields.count { it.type == "I" } >= 2 && clazz.fields.any { it.type == "Z" } && clazz.fields.count { it.type.startsWith("L") } >= 4
                } ?: continue

                val viewIdField = pageSpecClassDef.fields.firstOrNull {
                    it.type == "I"
                } ?: continue

                val pageField = pageSpecClassDef.fields.firstOrNull { field ->
                    val type = field.type
                    type.startsWith("L") && !type.startsWith("Ljava/") && !type.startsWith("Lkotlin/") && !type.startsWith("Landroid/") && !type.startsWith("Landroidx/") && !type.contains("Function")
                } ?: continue

                val specMapField = controller.fields.firstOrNull { field ->
                    (field.type == "Ljava/util/LinkedHashMap;" || field.type == "Ljava/util/Map;") && controller.methods.any { method ->
                        val refs = method.referencedFields()
                        refs.any {
                            it.definingClass == controller.type && it.name == field.name
                        } && refs.any {
                            it.definingClass == pageSpecClassDef.type
                        }
                    }
                } ?: continue

                val targetPageType = pageField.type
                val (managerField, managerClass, currentMethod) = controller.fields.mapNotNull { field ->
                    val mgrClass = getClass(field.type) ?: return@mapNotNull null
                    val currentM = mgrClass.methods.singleOrNull {
                        it.name != "<init>" && it.name != "<clinit>" && it.parameterTypes.isEmpty() && it.returnType == targetPageType
                    } ?: return@mapNotNull null
                    Triple(field, mgrClass, currentM)
                }.firstOrNull() ?: continue

                val navItemMethods = controller.methods.filter { method ->
                    method.parameterTypes.any { it == pageSpecClassDef.type } && method.returnType.startsWith("L")
                }

                val containerIds = classes.asSequence().flatMap { it.methods.asSequence() }.filter { method ->
                    navItemMethods.any { navMethod ->
                        method.referencedMethods().any { ref ->
                            ref.definingClass == controller.type && ref.name == navMethod.name && ref.parameterTypes == navMethod.parameterTypes
                        }
                    }
                }.flatMap { it.findSetViewIds().asSequence() }.filter { it in 0x7f010000..0x7f1f0000 }.distinct().toList()

                val titleCallback = navItemMethods.asSequence().flatMap { it.referencedMethods().asSequence() }.filter { it.name == "<init>" && it.parameterTypes.size == 2 && it.parameterTypes[1] == "I" }.mapNotNull { getClass(it.definingClass) }.distinctBy { it.type }.filter { clazz ->
                    clazz.interfaces.any { it.contains("/Consumer;") } && clazz.fields.any { it.type == "I" } && clazz.fields.any { it.type.startsWith("L") && !it.type.startsWith("Ljava/") } && clazz.methods.any { method ->
                        method.parameterTypes == listOf("Ljava/lang/Object;") && method.returnType == "V"
                    }
                }.singleOrNull()

                val containerId = containerIds.singleOrNull() ?: continue

                subscriberClass.set(subscriber.getClassName())
                subscriberControllerField.set(controllerField.name)
                controllerClass.set(controller.getClassName())
                navigationManagerField.set(managerField.name)
                navigationManagerClass.set(managerClass.getClassName())
                currentPageMethod.set(currentMethod.name)
                pageType.set(targetPageType)
                pageSpecMapField.set(specMapField.name)
                pageSpecClass.set(pageSpecClassDef.getClassName())
                pageSpecPageField.set(pageField.name)
                pageSpecViewIdField.set(viewIdField.name)
                navigationContainer.set("0x${containerId.toString(16)}")

                titleCallback?.let { clazz ->
                    val branch = clazz.fields.singleOrNull { it.type == "I" }
                    val target = clazz.fields.singleOrNull { it.type.startsWith("L") && !it.type.startsWith("Ljava/") }
                    val accept = clazz.methods.singleOrNull { it.parameterTypes == listOf("Ljava/lang/Object;") && it.returnType == "V" }

                    if (branch != null && target != null && accept != null) {
                        navigationTitleCallbackClass.set(clazz.getClassName())
                        navigationTitleBranchField.set(branch.name)
                        navigationTitleViewField.set(target.name)
                        navigationTitleCallbackMethod.set(accept.name)
                    }
                }

                navigationCallbacks.get()?.apply {
                    clear()
                    callbacks.forEachIndexed { index, method ->
                        put("callback$index", method.name)
                    }
                }

                return@mapper
            }
        }
    }
}