package me.rhunk.snapenhance.common.scripting.ui.components.impl

import me.rhunk.snapenhance.common.scripting.ui.components.Node
import me.rhunk.snapenhance.common.scripting.ui.components.NodeType

class CardStyleNode(type: NodeType) : Node(type) {
    fun cardStyle(style: String): CardStyleNode {
        assert(style in listOf("elevated", "outlined")) { "Invalid card style. Must be one of: elevated or outlined" }
        attributes["cardStyle"] = style
        return this
    }

    fun cardShape(topStart: Int, topEnd: Int, bottomEnd: Int, bottomStart: Int): CardStyleNode {
        attributes["cardShape"] = listOf(topStart, topEnd, bottomEnd, bottomStart)
        return this
    }
}