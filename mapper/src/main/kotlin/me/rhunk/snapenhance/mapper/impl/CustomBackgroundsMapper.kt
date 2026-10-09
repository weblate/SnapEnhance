/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.findConstString
import me.rhunk.snapenhance.mapper.ext.getClassName

class CustomBackgroundMapper : AbstractClassMapper("CustomBackground") {
    val backgroundProviderClass = classReference("backgroundProviderClass")
    val pullToRefreshFragmentClass = classReference("pullToRefreshFragmentClass")
    val staticBackgroundMethods = map("staticBackgroundMethods")
    val headerBackgroundMethod = string("headerBackgroundMethod")
    val contentBackgroundMethod = string("contentBackgroundMethod")
    val setupMethod = string("setupMethod")

    init {
        mapper {
            for (clazz in classes) {
                val headerMethod = clazz.methods.firstOrNull {
                    it.parameterTypes.size == 2 && it.parameterTypes[0] == "I" && it.parameterTypes[1] == "I" && it.returnType == "Landroid/graphics/drawable/Drawable;" && it.implementation?.findConstString("createHeaderBackgroundDrawable") == true
                } ?: continue

                val contentMethod = clazz.methods.firstOrNull {
                    it.parameterTypes.size == 2 && it.parameterTypes[0] == "I" && it.parameterTypes[1] == "I" && it.returnType == "Landroid/graphics/drawable/Drawable;" && it.implementation?.findConstString("createRecyclerViewBackgroundDrawable") == true
                } ?: continue

                val staticMethods = clazz.methods.filter {
                    it.parameterTypes.isEmpty() && it.returnType == "Landroid/graphics/drawable/Drawable;"
                }

                if (staticMethods.size != 3) continue

                backgroundProviderClass.set(clazz.getClassName())
                headerBackgroundMethod.set(headerMethod.name)
                contentBackgroundMethod.set(contentMethod.name)
                staticBackgroundMethods.get()?.apply {
                    clear()
                    staticMethods.forEachIndexed { index, method ->
                        this[index.toString()] = method.name
                    }
                }

                return@mapper
            }
        }

        mapper {
            val clazz = classes.firstOrNull {
                it.type == "Lcom/snap/ui/ptr/PullToRefreshFragment;"
            } ?: return@mapper

            val setup = clazz.methods.firstOrNull {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == "Landroid/view/View;" && it.returnType == "V"
            } ?: return@mapper

            pullToRefreshFragmentClass.set(clazz.getClassName())
            setupMethod.set(setup.name)
        }
    }
}