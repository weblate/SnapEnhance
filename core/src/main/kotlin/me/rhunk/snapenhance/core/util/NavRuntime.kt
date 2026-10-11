/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.core.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.collections.get

class NavRuntime(
    val controller: Any,
    val manager: Any,
    val currentPageMethod: Method,
    val pageSpecMapField: Field,
    val pageSpecViewIdField: Field
) {
    fun selectedViewId(): Int? = runCatching {
        val page = currentPageMethod.invoke(manager) ?: return null
        val spec = (pageSpecMapField.get(controller) as? Map<*, *>)?.get(page) ?: return null
        pageSpecViewIdField.getInt(spec)
    }.getOrNull()
}