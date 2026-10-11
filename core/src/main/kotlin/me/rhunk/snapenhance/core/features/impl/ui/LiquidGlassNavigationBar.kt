/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
 */

package me.rhunk.snapenhance.core.features.impl.ui

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.children
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.rhunk.snapenhance.common.data.NavItem
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.ui.liquidglass.CoreLiquidNavigationBar
import me.rhunk.snapenhance.common.ui.liquidglass.LiquidNavigationItem
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.extractIcon
import me.rhunk.snapenhance.core.ui.extractText
import me.rhunk.snapenhance.core.util.NavRuntime
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.findField
import me.rhunk.snapenhance.core.util.hook.findMethod
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.mapper.impl.NavigationBarMapper
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArraySet

class LiquidGlassNavigationBar : Feature("Liquid Glass Navigation Bar") {
    private val overriddenTag = 0x53454E48
    private var containerViewId: Int? = null
    private var runtime: NavRuntime? = null
    private var isNavigationBarOverridden = false
    private val listeners = CopyOnWriteArraySet<(Int) -> Unit>()
    private val navigationTitles = WeakHashMap<View, String>()
    private val titleListeners = CopyOnWriteArraySet<(View, String) -> Unit>()

    override fun init() {
        if (!context.config.userInterface.liquidGlassNavigationBar.get()) return

        if (!isNavigationBarOverridden) {
            synchronized(this) {
                if (!isNavigationBarOverridden) {
                    context.mappings.useMapper(NavigationBarMapper::class) {
                        navigationContainer.get()?.let { id ->
                            containerViewId = id.removePrefix("0x").removePrefix("0X").toLongOrNull(16)?.toInt() ?: id.toIntOrNull()
                        }

                        val subscriberClass = subscriberClass.get() ?: return@useMapper
                        val controllerClass = controllerClass.get() ?: return@useMapper

                        val fetchSubscriberControllerField = subscriberClass.findField(subscriberControllerField.get() ?: "") ?: return@useMapper
                        val fetchNavigationManagerField = controllerClass.findField(navigationManagerField.get() ?: "") ?: return@useMapper
                        val fetchCurrentPageMethod = navigationManagerClass.get()?.findMethod(currentPageMethod.get() ?: "", 0) ?: return@useMapper
                        val fetchPageSpecMapField = controllerClass.findField(pageSpecMapField.get() ?: "") ?: return@useMapper
                        val fetchPageSpecViewIdField = pageSpecClass.get()?.findField(pageSpecViewIdField.get() ?: "") ?: return@useMapper
                        val fetchNavigationCallbacks = navigationCallbacks.get()?.values?.filterNotNull()?.toSet().orEmpty()

                        fetchNavigationCallbacks.forEach { callback ->
                            subscriberClass.declaredMethods
                                .filter { it.name == callback && it.parameterCount == 1 && it.returnType == Void.TYPE }
                                .forEach { method ->
                                    method.hook(HookStage.AFTER) { hook ->
                                        val instance = hook.thisObject<Any>()
                                        val subscriberControllerFieldInstance = runCatching { fetchSubscriberControllerField.get(instance) }.getOrNull() ?: return@hook
                                        if (!controllerClass.isInstance(subscriberControllerFieldInstance)) return@hook

                                        if (runtime?.controller !== subscriberControllerFieldInstance) {
                                            val navigationManagerFieldInstance = runCatching { fetchNavigationManagerField.get(subscriberControllerFieldInstance) }.getOrNull() ?: return@hook
                                            runtime = NavRuntime(subscriberControllerFieldInstance, navigationManagerFieldInstance, fetchCurrentPageMethod, fetchPageSpecMapField, fetchPageSpecViewIdField)
                                        }

                                        runtime?.selectedViewId()?.let { id ->
                                            listeners.forEach { runCatching { it(id) } }
                                        }
                                    }
                                }
                        }

                        runCatching {
                            val titleCallbackClass = navigationTitleCallbackClass.get() ?: return@runCatching
                            val branchField = titleCallbackClass.findField(navigationTitleBranchField.get() ?: "") ?: return@runCatching
                            val targetField = titleCallbackClass.findField(navigationTitleViewField.get() ?: "") ?: return@runCatching
                            val callbackName = navigationTitleCallbackMethod.get() ?: return@runCatching
                            val callbackMethod = titleCallbackClass.declaredMethods.singleOrNull {
                                it.name == callbackName && it.parameterCount == 1 && it.parameterTypes[0] == Any::class.java
                            } ?: return@runCatching
                            branchField.isAccessible = true
                            targetField.isAccessible = true
                            callbackMethod.hook(HookStage.AFTER) { hook ->
                                val callback = hook.thisObject<Any>()
                                if (branchField.getInt(callback) != 0) return@hook
                                val target = targetField.get(callback) as? View ?: return@hook
                                val title = target.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return@hook

                                synchronized(navigationTitles) { navigationTitles[target] = title }

                                titleListeners.forEach { listener ->
                                    runCatching { listener(target, title) }
                                }
                            }
                        }.onFailure { context.log.error("Navigation title hook failed: $it") }

                        isNavigationBarOverridden = true
                    }
                }
            }
        }

        val targetContainerId = containerViewId ?: run {
            context.log.error("targetContainerId does not match containerViewId")
            return
        }

        context.event.subscribe(AddViewEvent::class) { event ->
            val container = event.view.takeIf { it.id == targetContainerId } as? ViewGroup ?: return@subscribe
            val viewGroup = event.parent as? ViewGroup ?: return@subscribe
            if (container.getTag(overriddenTag) == true) return@subscribe

            viewGroup.post {
                if (container.getTag(overriddenTag) == true) return@post

                listOf(container, viewGroup).forEach {
                    it.clipChildren = false
                    it.clipToPadding = false
                }

                var currentView: ViewParent? = viewGroup.parent
                var depth = 0
                while (currentView is ViewGroup && depth++ < 6) {
                    currentView.clipChildren = false
                    currentView.clipToPadding = false
                    currentView = currentView.parent
                }

                val navItems = mutableStateListOf<NavItem>()

                fun registerChild(child: View) = child.post {
                    if (navItems.any { it.targetView === child }) return@post
                    val title = synchronized(navigationTitles) { navigationTitles[child] } ?: child.extractText()
                    navItems += NavItem(navItems.size, title, child.extractIcon(), child)
                    child.alpha = 0f
                    runtime?.selectedViewId()?.let { id ->
                        listeners.forEach { runCatching { it(id) } }
                    }
                }

                container.children.forEach(::registerChild)
                container.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        child?.let(::registerChild)
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        child?.let { child -> navItems.removeAll { it.targetView === child } }
                    }
                })

                val composeView = createComposeView(viewGroup.context) {
                    var selectedIndex by remember { mutableIntStateOf(-1) }
                    val hostView = LocalView.current

                    DisposableEffect(Unit) {
                        val titleListener: (View, String) -> Unit = { target, title ->
                            viewGroup.post {
                                val index = navItems.indexOfFirst { it.targetView === target }
                                if (index >= 0 && navItems[index].title != title) {
                                    navItems[index] = navItems[index].copy(title = title)
                                }
                            }
                        }
                        titleListeners += titleListener
                        onDispose { titleListeners -= titleListener }
                    }

                    DisposableEffect(navItems.size) {
                        val listener: (Int) -> Unit = { id ->
                            viewGroup.post {
                                val idx = navItems.indexOfFirst { it.targetView.id == id }
                                if (idx >= 0 && idx != selectedIndex) selectedIndex = idx
                            }
                        }

                        listeners += listener
                        runtime?.selectedViewId()?.let(listener)
                        onDispose { listeners -= listener }
                    }

                    if (navItems.isNotEmpty()) {
                        val displayedIndex = selectedIndex.takeIf {
                            it in navItems.indices
                        } ?: navItems.indexOfFirst {
                            it.title?.equals("Camera", ignoreCase = true) == true
                        }.takeIf { it >= 0 } ?: 0

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            if (awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }) {
                                                hostView.parent?.requestDisallowInterceptTouchEvent(true)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            CoreLiquidNavigationBar(
                                selectedTabIndex = { if (selectedIndex in navItems.indices) selectedIndex else displayedIndex },
                                onTabSelected = { index -> navItems.getOrNull(index)?.targetView?.performClick() },
                                backdrop = rememberLayerBackdrop(),
                                tabsCount = navItems.size,
                                primaryColor = context.userInterface.colorPrimary,
                                surfaceContainerColor = context.userInterface.actionSheetBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                navItems.forEach { item ->
                                    LiquidNavigationItem(
                                        onClick = { item.targetView.performClick() }
                                    ) {
                                        item.icon?.let {
                                            Icon(
                                                bitmap = it,
                                                tint = Color(context.userInterface.colorPrimary),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        item.title?.let {
                                            Text(
                                                text = it,
                                                textAlign = TextAlign.Center,
                                                color = Color(context.userInterface.colorPrimary),
                                                softWrap = false,
                                                fontSize = 10.sp,
                                                modifier = Modifier.wrapContentWidth(unbounded = true)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                (composeView as? ViewGroup)?.apply {
                    clipChildren = false
                    clipToPadding = false
                }

                viewGroup.addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                composeView.bringToFront()
                container.setTag(overriddenTag, true)
                runtime?.selectedViewId()?.let { id ->
                    listeners.forEach { runCatching { it(id) } }
                }
            }
        }
    }
}