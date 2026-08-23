package me.rhunk.snapenhance.common.scripting.ui.components

import androidx.compose.ui.text.font.FontWeight

@Suppress("MemberVisibilityCanBePrivate")
open class Node(
    val type: NodeType,
) {
    lateinit var uiChangeDetection: (key: String, value: Any?) -> Unit

    val children = mutableListOf<Node>()
    val attributes = object: HashMap<String, Any?>() {
        override fun put(key: String, value: Any?): Any? {
            return super.put(key, value).also {
                if (::uiChangeDetection.isInitialized) {
                    uiChangeDetection(key, value)
                }
            }
        }
    }

    companion object {
        private val fontWeights = mapOf(
            "thin" to FontWeight.Thin,
            "extraLight" to FontWeight.ExtraLight,
            "light" to FontWeight.Light,
            "normal" to FontWeight.Normal,
            "medium" to FontWeight.Medium,
            "semiBold" to FontWeight.SemiBold,
            "bold" to FontWeight.Bold,
            "extraBold" to FontWeight.ExtraBold,
            "black" to FontWeight.Black
        )

        private val textStyles = listOf(
            "displayLarge", "displayMedium", "displaySmall",
            "headlineLarge", "headlineMedium", "headlineSmall",
            "titleLarge", "titleMedium", "titleSmall",
            "bodyLarge", "bodyMedium", "bodySmall",
            "labelLarge", "labelMedium", "labelSmall"
        )
    }

    init {
        visibility("visible")
    }

    fun setAttribute(key: String, value: Any?) {
        attributes[key] = value
    }

    fun fillMaxWidth(): Node {
        attributes["fillMaxWidth"] = true
        return this
    }

    fun fillMaxHeight(): Node {
        attributes["fillMaxHeight"] = true
        return this
    }

    fun label(text: String): Node {
        attributes["label"] = text
        return this
    }

    fun padding(vararg values: Int): Node {
        attributes["padding"] = values.toList()
        return this
    }

    fun fontSize(size: Int): Node {
        attributes["fontSize"] = size
        return this
    }

    fun fontWeight(weight: String): Node {
        attributes["fontWeight"] = fontWeights[weight] ?: throw IllegalArgumentException("Invalid Weight")
        return this
    }

    fun textStyle(style: String): Node {
        if (style !in textStyles) { throw IllegalArgumentException("Invalid Style") }
        attributes["textStyle"] = style
        return this
    }

    fun lineHeight(height: Int): Node {
        attributes["lineHeight"] = height
        return this
    }

    fun color(color: Long): Node {
        attributes["color"] = color
        return this
    }

    fun visibility(state: String) {
        assert(state == "visible" || state == "invisible" || state == "gone") { "Invalid visibility state. Must be one of: visible, invisible, gone" }
        attributes["visibility"] = state
    }
}