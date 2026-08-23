@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.common.scripting.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.logger.AbstractLogger
import me.rhunk.snapenhance.common.scripting.ui.components.Node
import me.rhunk.snapenhance.common.scripting.ui.components.NodeType
import me.rhunk.snapenhance.common.scripting.ui.components.impl.ActionNode
import me.rhunk.snapenhance.common.scripting.ui.components.impl.ActionType
import kotlin.math.abs

@SuppressLint("SetJavaScriptEnabled")
@Composable
@Suppress("UNCHECKED_CAST")
private fun DrawNode(node: Node) {
    val coroutineScope = rememberCoroutineScope()
    val cachedAttributes = remember { mutableStateMapOf(*node.attributes.toList().toTypedArray()) }

    node.uiChangeDetection = { key, value ->
        coroutineScope.launch {
            cachedAttributes[key] = value
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            node.uiChangeDetection = { _, _ -> }
        }
    }

    val arrangement = cachedAttributes["arrangement"]
    val alignment = cachedAttributes["alignment"]
    val spacing = cachedAttributes["spacing"]?.toString()?.toInt()?.let { abs(it) }
    val textStyle = when (cachedAttributes["textStyle"] as? String ?: "bodyMedium") {
        "displayLarge" -> MaterialTheme.typography.displayLarge
        "displayMedium" -> MaterialTheme.typography.displayMedium
        "displaySmall" -> MaterialTheme.typography.displaySmall
        "headlineLarge" -> MaterialTheme.typography.headlineLarge
        "headlineMedium" -> MaterialTheme.typography.headlineMedium
        "headlineSmall" -> MaterialTheme.typography.headlineSmall
        "titleLarge" -> MaterialTheme.typography.titleLarge
        "titleMedium" -> MaterialTheme.typography.titleMedium
        "titleSmall" -> MaterialTheme.typography.titleSmall
        "bodyLarge" -> MaterialTheme.typography.bodyLarge
        "bodySmall" -> MaterialTheme.typography.bodySmall
        "labelLarge" -> MaterialTheme.typography.labelLarge
        "labelMedium" -> MaterialTheme.typography.labelMedium
        "labelSmall" -> MaterialTheme.typography.labelSmall
        else -> MaterialTheme.typography.bodyMedium
    }

    val paddingValues = remember(cachedAttributes["padding"]) {
        val values = cachedAttributes["padding"] as? List<Int> ?: emptyList()

        when (values.size) {
            1 -> PaddingValues(all = values[0].dp)
            2 -> PaddingValues(
                horizontal = values[0].dp,
                vertical = values[1].dp
            )
            4 -> PaddingValues(
                start = values[0].dp,
                top = values[1].dp,
                end = values[2].dp,
                bottom = values[3].dp
            )
            else -> PaddingValues(0.dp)
        }
    }


    val rowColumnModifier = Modifier
        .then(if (cachedAttributes["fillMaxWidth"] as? Boolean == true) Modifier.fillMaxWidth() else Modifier)
        .then(if (cachedAttributes["fillMaxHeight"] as? Boolean == true) Modifier.fillMaxHeight() else Modifier)
        .padding(paddingValues)

    fun runCallbackSafe(callback: () -> Unit) {
        runCatching {
            callback()
        }.onFailure {
            AbstractLogger.directError("Error running callback", it)
        }
    }

    @Composable
    fun NodeLabel() {
        Text(
            text = cachedAttributes["label"] as String,
            fontSize = cachedAttributes["fontSize"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified,
            fontWeight = cachedAttributes["fontWeight"] as? FontWeight,
            color = (cachedAttributes["color"] as? Long)?.let { Color(it) } ?: Color.Unspecified,
            lineHeight = cachedAttributes["lineHeight"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified,
            style = textStyle
        )
    }

    if (cachedAttributes["visibility"] != "gone") {
        AnimatedVisibility(
            visible = cachedAttributes["visibility"] != "invisible",
        ) {
            when (node.type) {
                NodeType.ACTION -> {
                    when ((node as ActionNode).actionType) {
                        ActionType.LAUNCHED -> {
                            LaunchedEffect(node.key) {
                                runCallbackSafe {
                                    node.callback()
                                }
                            }
                        }
                        ActionType.DISPOSE -> {
                            DisposableEffect(Unit) {
                                onDispose {
                                    runCallbackSafe {
                                        node.callback()
                                    }
                                }
                            }
                        }
                    }
                }
                NodeType.COLUMN -> {
                    Column(
                        verticalArrangement = arrangement as? Arrangement.Vertical ?: spacing?.let { Arrangement.spacedBy(it.dp) } ?: Arrangement.Top,
                        horizontalAlignment = alignment as? Alignment.Horizontal ?: Alignment.Start,
                        modifier = rowColumnModifier
                    ) {
                        node.children.forEach { child ->
                            DrawNode(child)
                        }
                    }
                }
                NodeType.ROW -> {
                    Row(
                        horizontalArrangement = arrangement as? Arrangement.Horizontal ?: spacing?.let { Arrangement.spacedBy(it.dp) } ?: Arrangement.SpaceBetween,
                        verticalAlignment = alignment as? Alignment.Vertical ?: Alignment.CenterVertically,
                        modifier = rowColumnModifier
                    ) {
                        node.children.forEach { child ->
                            DrawNode(child)
                        }
                    }
                }
                NodeType.TEXT -> NodeLabel()
                NodeType.SWITCH -> {
                    val isChecked = cachedAttributes["state"] as? Boolean ?: false

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { state ->
                            runCallbackSafe {
                                node.setAttribute("state", state)
                                (cachedAttributes["callback"] as? (Boolean) -> Unit)?.let { it(state) }
                            }
                        }
                    )
                }
                NodeType.SLIDER -> {
                    val sliderValue = (cachedAttributes["value"] as? Number)?.toFloat() ?: 0f

                    Slider(
                        value = sliderValue,
                        onValueChange = { value ->
                            runCallbackSafe {
                                node.setAttribute("value", value.toInt())
                                (cachedAttributes["callback"] as? (Int) -> Unit)?.let { it(value.toInt()) }
                            }
                        },
                        valueRange = (cachedAttributes["min"] as? Int ?: 0).toFloat()..(cachedAttributes["max"] as? Int ?: 100).toFloat(),
                        steps = cachedAttributes["step"] as? Int ?: 0,
                    )
                }
                NodeType.BUTTON -> {
                    val buttonSize = when (cachedAttributes["buttonSize"]?.toString() ?: "default") {
                        "extraSmall" -> ButtonDefaults.ExtraSmallContainerHeight
                        "medium" -> ButtonDefaults.MediumContainerHeight
                        "large" -> ButtonDefaults.LargeContainerHeight
                        "extraLarge" -> ButtonDefaults.ExtraLargeContainerHeight
                        else -> ButtonDefaults.MinHeight
                    }
                    val onClick: () -> Unit = {
                        runCallbackSafe {
                            (cachedAttributes["callback"] as? () -> Unit)?.invoke()
                        }
                    }

                    @Composable
                    fun ButtonLabel() {
                        Text(
                            text = cachedAttributes["label"]?.toString() ?: "",
                            fontSize = cachedAttributes["fontSize"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified,
                            fontWeight = cachedAttributes["fontWeight"] as? FontWeight ?: FontWeight.Normal,
                            color = (cachedAttributes["color"] as? Long)?.let { Color(it) } ?: Color.Unspecified,
                            lineHeight = cachedAttributes["lineHeight"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified,
                            style = ButtonDefaults.textStyleFor(buttonSize)
                        )
                    }

                    when (cachedAttributes["buttonStyle"]?.toString() ?: "default") {
                        "elevated" -> ElevatedButton(
                            onClick = onClick,
                            modifier = Modifier.heightIn(buttonSize),
                            contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = false),
                            shapes = ButtonDefaults.shapes()
                        ) { ButtonLabel() }

                        "tonal" -> FilledTonalButton(
                            onClick = onClick,
                            contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = false),
                            modifier = Modifier.heightIn(buttonSize),
                            shapes = ButtonDefaults.shapes()
                        ) { ButtonLabel() }

                        "outlined" -> OutlinedButton(
                            onClick = onClick,
                            contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = false),
                            modifier = Modifier.heightIn(buttonSize),
                            shapes = ButtonDefaults.shapes()
                        ) { ButtonLabel() }

                        "text" -> TextButton(
                            onClick = onClick,
                            contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = false),
                            modifier = Modifier.heightIn(buttonSize),
                            shapes = ButtonDefaults.shapes()
                        ) { ButtonLabel() }

                        else -> Button(
                            onClick = onClick,
                            contentPadding = ButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = false),
                            modifier = Modifier.heightIn(buttonSize),
                            shapes = ButtonDefaults.shapes()
                        ) { ButtonLabel() }
                    }
                }
                NodeType.ICON -> {
                    val rawIcon = cachedAttributes["icon"]?.toString() ?: "Help"
                    val size = (cachedAttributes["size"] as? Int ?: 24).dp
                    val iconName = rawIcon.replaceFirstChar { it.uppercase() }

                    val iconVector = remember(iconName) {
                        try {
                            val cl = Class.forName("androidx.compose.material.icons.rounded.${iconName}Kt")
                            val method = cl.getDeclaredMethod("get$iconName", Icons.Rounded::class.java)
                            method.invoke(null, Icons.Rounded) as ImageVector
                        } catch (_: Exception) {
                            Icons.Rounded.QuestionMark
                        }
                    }

                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = (cachedAttributes["color"] as? Long)?.let { Color(it) } ?: LocalContentColor.current,
                    )
                }
                NodeType.ICON_BUTTON -> {
                    val rawIcon = cachedAttributes["icon"]?.toString() ?: "Help"
                    val iconName = rawIcon.replaceFirstChar { it.uppercase() }
                    val iconButtonSize = when (cachedAttributes["iconButtonSize"]?.toString() ?: "default") {
                        "extraSmall" -> IconButtonDefaults.extraSmallContainerSize()
                        "medium" -> IconButtonDefaults.mediumContainerSize()
                        "large" -> IconButtonDefaults.largeContainerSize()
                        "extraLarge" -> IconButtonDefaults.extraLargeContainerSize()
                        else -> IconButtonDefaults.smallContainerSize()
                    }

                    val onClick: () -> Unit = {
                        runCallbackSafe {
                            (cachedAttributes["callback"] as? () -> Unit)?.invoke()
                        }
                    }

                    val iconVector = remember(iconName) {
                        try {
                            val cl = Class.forName("androidx.compose.material.icons.rounded.${iconName}Kt")
                            val method = cl.getDeclaredMethod("get$iconName", Icons.Rounded::class.java)
                            method.invoke(null, Icons.Rounded) as ImageVector
                        } catch (_: Exception) {
                            Icons.Rounded.QuestionMark
                        }
                    }

                    @Composable
                    fun IconButtonIcon() {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(if (iconButtonSize.height > 50.dp) 32.dp else 24.dp),
                            tint = (cachedAttributes["color"] as? Long)?.let { Color(it) } ?: LocalContentColor.current,
                        )
                    }

                    when (cachedAttributes["iconButtonStyle"]?.toString()) {
                        "filled" -> FilledIconButton(
                            onClick = onClick,
                            modifier = Modifier.size(iconButtonSize),
                            shapes = IconButtonDefaults.shapes()
                        ) { IconButtonIcon() }

                        "tonal" -> FilledTonalIconButton(
                            onClick = onClick,
                            modifier = Modifier.size(iconButtonSize),
                            shapes = IconButtonDefaults.shapes()
                        ) { IconButtonIcon() }

                        "outlined" -> OutlinedIconButton(
                            onClick = onClick,
                            modifier = Modifier.size(iconButtonSize),
                            shapes = IconButtonDefaults.shapes()
                        ) { IconButtonIcon() }

                        else -> IconButton(
                            onClick = onClick,
                            modifier = Modifier.size(iconButtonSize),
                            shapes = IconButtonDefaults.shapes()
                        ) { IconButtonIcon() }
                    }
                }
                NodeType.PROGRESS_INDICATOR -> {
                    val progressFloat = (cachedAttributes["progress"] as? Number)?.toFloat()?.let {
                        (it / 100f).coerceIn(0f, 1f)
                    }
                    val modifier = Modifier
                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                        .padding(paddingValues)

                    when (cachedAttributes["variant"].toString()) {
                        "circular" -> {
                            if (progressFloat != null) {
                                CircularWavyProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = modifier
                                )
                            } else {
                                CircularWavyProgressIndicator(
                                    modifier = modifier
                                )
                            }
                        }
                        "linear" -> {
                            if (progressFloat != null) {
                                LinearWavyProgressIndicator(
                                    progress = { progressFloat },
                                    modifier = modifier
                                )
                            } else {
                                LinearWavyProgressIndicator(
                                    modifier = modifier
                                )
                            }
                        }
                    }
                }
                NodeType.LOADING_INDICATOR -> {
                    when (cachedAttributes["variant"].toString()) {
                        "contained" -> ContainedLoadingIndicator()
                        "nonContained" -> LoadingIndicator()
                    }
                }
                NodeType.CHECKBOX -> {
                    val isChecked = cachedAttributes["state"] as? Boolean ?: false

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { state ->
                            runCallbackSafe {
                                node.setAttribute("state", state)
                                (cachedAttributes["callback"] as? (Boolean) -> Unit)?.invoke(state)
                            }
                        }
                    )
                }
                NodeType.RADIO_BUTTON -> {
                    val isSelected = cachedAttributes["state"] as? Boolean ?: false

                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            runCallbackSafe {
                                val newState = !isSelected
                                node.setAttribute("state", newState)
                                (cachedAttributes["callback"] as? (Boolean) -> Unit)?.invoke(newState)
                            }
                        }
                    )
                }
                NodeType.CARD -> {
                    val shapeList = cachedAttributes["cardShape"] as? List<Int> ?: listOf(12, 12, 12, 12)

                    val shape = RoundedCornerShape(
                        topStart = shapeList[0].dp,
                        topEnd = shapeList[1].dp,
                        bottomEnd = shapeList[2].dp,
                        bottomStart = shapeList[3].dp
                    )

                    val callbackAction: (() -> Unit)? = (cachedAttributes["callback"] as? () -> Unit)

                    when (cachedAttributes["cardStyle"]?.toString()) {
                        "elevated" ->
                            if (callbackAction != null) {
                                ElevatedCard(
                                    onClick = { runCallbackSafe(callbackAction) },
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            } else {
                                ElevatedCard(
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            }
                        "outlined" ->
                            if (callbackAction != null) {
                                OutlinedCard(
                                    onClick = { runCallbackSafe(callbackAction) },
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            } else {
                                OutlinedCard(
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            }
                        else -> {
                            if (callbackAction != null) {
                                Card(
                                    onClick = { runCallbackSafe(callbackAction) },
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier
                                        .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                        .padding(paddingValues),
                                    shape = shape
                                ) {
                                    node.children.forEach { child ->
                                        DrawNode(child)
                                    }
                                }
                            }
                        }
                    }
                }
                NodeType.DIVIDER -> {
                    when (cachedAttributes["variant"].toString()) {
                        "horizontal" -> HorizontalDivider(
                            modifier = Modifier
                                .then(if (cachedAttributes["fillMaxWidth"] == true) Modifier.fillMaxWidth() else Modifier)
                                .padding(paddingValues),
                        )
                        "vertical" -> VerticalDivider(
                            modifier = Modifier
                                .then(
                                    if (cachedAttributes["fillMaxHeight"] == true) Modifier.fillMaxHeight()
                                    else Modifier.height(24.dp)
                                )
                                .padding(paddingValues),
                        )
                    }
                }
                NodeType.WEB_COMPONENT -> {
                    val html = cachedAttributes["html"]?.toString() ?: ""
                    val css = cachedAttributes["css"]?.toString() ?: ""
                    val js = cachedAttributes["js"]?.toString() ?: ""

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )

                                setBackgroundColor(0)
                                settings.javaScriptEnabled = true
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun callback(data: String) {
                                        runCallbackSafe {
                                            (cachedAttributes["callback"] as? (String) -> Unit)?.invoke(data)
                                        }
                                    }
                                }, "webComponentBridge")
                            }
                        },

                        update = { webView ->
                            val fullHtml = """
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                    <style>
                                        body { margin: 0; padding: 0; overflow: hidden; display: block; -webkit-tap-highlight-color: transparent; outline: none;}
                                        $css
                                    </style>
                                </head>
                                <body>
                                    <div id="content_wrapper">$html</div>
                                    <script>
                                        $js
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(paddingValues)
                    )
                }
                NodeType.TEXT_INPUT -> {
                    val textInputValue = cachedAttributes["value"]?.toString() ?: ""

                    TextField(
                        value = textInputValue,
                        readOnly = cachedAttributes["readonly"] as? Boolean ?: false,
                        singleLine = cachedAttributes["singleLine"] as? Boolean ?: true,
                        maxLines = cachedAttributes["maxLines"] as? Int ?: 1,
                        onValueChange = { value ->
                            runCallbackSafe {
                                node.setAttribute("value", value)
                                (cachedAttributes["callback"] as? (String) -> Unit)?.let { it(value) }
                            }
                        },
                        placeholder = {
                            Text(
                                text = cachedAttributes["placeholder"].toString(),
                                fontSize = cachedAttributes["fontSize"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified,
                                fontWeight = cachedAttributes["fontWeight"] as? FontWeight ?: FontWeight.Normal,
                                color = (cachedAttributes["color"] as? Long)?.let { Color(it) } ?: Color.Unspecified,
                                lineHeight = cachedAttributes["lineHeight"]?.toString()?.toIntOrNull()?.sp ?: TextUnit.Unspecified
                            )
                        },
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ScriptInterface(interfaceBuilder: InterfaceBuilder) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        interfaceBuilder.nodes.forEach { node ->
            DrawNode(node)
        }

        DisposableEffect(Unit) {
            onDispose {
                runCatching {
                    interfaceBuilder.onDisposeCallback?.invoke()
                }.onFailure {
                    AbstractLogger.directError("Error running onDisposed callback", it)
                }
            }
        }
    }
}