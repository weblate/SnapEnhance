package me.rhunk.snapenhance.ui.manager.data

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuAction(
    val name: String,
    val icon: ImageVector,
    val action: () -> Unit
)