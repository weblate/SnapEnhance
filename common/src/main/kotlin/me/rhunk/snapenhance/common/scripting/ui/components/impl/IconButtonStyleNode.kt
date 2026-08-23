package me.rhunk.snapenhance.common.scripting.ui.components.impl

import me.rhunk.snapenhance.common.scripting.ui.components.Node
import me.rhunk.snapenhance.common.scripting.ui.components.NodeType

class IconButtonStyleNode(type: NodeType) : Node(type) {
    fun iconButtonStyle(style: String): IconButtonStyleNode {
        assert(style in listOf("tonal", "outlined", "filled")) { "Invalid button style. Must be one of: tonal, outlined or filled" }
        attributes["iconButtonStyle"] = style
        return this
    }

    fun iconButtonSize(size: String): IconButtonStyleNode {
        assert(size in listOf("extraSmall", "medium", "large", "extraLarge")) { "Invalid button size. Must be one of: extraSmall, medium, large or extraLarge" }
        attributes["iconButtonSize"] = size
        return this
    }
}