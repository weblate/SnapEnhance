package me.rhunk.snapenhance.common.data

import android.view.View
import androidx.compose.ui.graphics.ImageBitmap

data class NavItem(
    val index: Int,
    val title: String?,
    val icon: ImageBitmap?,
    val targetView: View
)