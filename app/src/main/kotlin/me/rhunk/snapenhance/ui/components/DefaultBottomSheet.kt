/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.ui.components

import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import me.rhunk.snapenhance.common.ui.bottomSheetShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    sheetGesturesEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        modifier = modifier,
        sheetState = sheetState,
        properties = properties,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = bottomSheetShape,
    ) {
        // https://issuetracker.google.com/issues/374013416#comment10
        (LocalView.current.parent as? DialogWindowProvider)?.window?.let { window ->
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 1.5f)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyColumnBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    sheetGesturesEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    DefaultBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        properties = properties,
        sheetGesturesEnabled = sheetGesturesEnabled,
    ) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            LazyColumn(
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    content()
                }
            }
        }
    }
}