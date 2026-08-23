package me.rhunk.snapenhance.common.scripting.ui.components.impl

import me.rhunk.snapenhance.common.scripting.ui.components.Node
import me.rhunk.snapenhance.common.scripting.ui.components.NodeType

class ButtonStyleNode(type: NodeType) : Node(type) {
    fun buttonStyle(style: String): ButtonStyleNode {
        assert(style in listOf("tonal", "outlined", "elevated", "text")) { "Invalid button style. Must be one of: tonal, outlined, elevated or text" }
        attributes["buttonStyle"] = style
        return this
    }

    fun buttonSize(size: String): ButtonStyleNode {
        assert(size in listOf("extraSmall", "medium", "large", "extraLarge")) { "Invalid button size. Must be one of: extraSmall, medium, large or extraLarge" }
        attributes["buttonSize"] = size
        return this
    }
}