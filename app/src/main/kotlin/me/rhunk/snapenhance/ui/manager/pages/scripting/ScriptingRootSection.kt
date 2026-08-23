@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.scripting

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.*
import coil.compose.AsyncImage
import me.rhunk.snapenhance.common.scripting.type.ModuleInfo
import me.rhunk.snapenhance.common.scripting.ui.EnumScriptInterface
import me.rhunk.snapenhance.common.scripting.ui.InterfaceManager
import me.rhunk.snapenhance.common.scripting.ui.ScriptInterface
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.common.util.ktx.getUrlFromClipboard
import me.rhunk.snapenhance.common.util.ktx.openLink
import me.rhunk.snapenhance.storage.isScriptEnabled
import me.rhunk.snapenhance.storage.setScriptEnabled
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.Base64ToImage
import me.rhunk.snapenhance.ui.util.chooseFolder
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import okhttp3.OkHttpClient

class ScriptingRootSection : Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper
    private var reloadTrigger by mutableStateOf(0)
    private val scriptOptions by lazy {
        listOf(
            translation["installed_scripts_tab"],
            translation["script_catalog_tab"]
        )
    }
    private var selectedIndex by mutableIntStateOf(0)
    private val showScriptCover = true // TODO: allow users to toggle this off in settings in the future.
    val localReloadDispatcher = AsyncUpdateDispatcher(updateOnFirstComposition = false)
    val okHttpClient by lazy { OkHttpClient() }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    @Composable
    private fun ImportRemoteScript(
        dismiss: () -> Unit
    ) {
        LazyColumnBottomSheet(
            onDismiss = { dismiss() }
        ) {
            var url by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            var isLoading by remember {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = context.translation["manager.dialogs.import_script_url.title"],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = context.translation["manager.dialogs.import_script_url.description"],
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                    },
                    label = {
                        Text(text = context.translation["manager.dialogs.import_script_url.url_hint"])
                    },
                    placeholder = { Text(
                        text = "https://raw.githubusercontent.com/{username}/{repo}/{branch}/{filename}.js",
                        modifier = Modifier.basicMarquee()
                    ) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            focusRequester.requestFocus()
                        }
                )
                LaunchedEffect(Unit) {
                    context.androidContext.getUrlFromClipboard()?.let {
                        url = it
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 5.dp)
                        .fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { dismiss() },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(text = context.translation["button.cancel"])
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        enabled = url.isNotBlank(),
                        onClick = {
                            isLoading = true
                            context.coroutineScope.launch {
                                runCatching {
                                    val moduleInfo = context.scriptManager.importFromUrl(url)
                                    context.shortToast("Script ${moduleInfo.name} imported!")
                                    localReloadDispatcher.dispatch()
                                    withContext(Dispatchers.Main) {
                                        dismiss()
                                    }
                                    return@launch
                                }.onFailure {
                                    context.log.error("Failed to import script", it)
                                    context.shortToast("Failed to import script. ${it.message}. Check logs for more details")
                                }
                                isLoading = false
                            }
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = context.translation["manager.dialogs.import_script_url.import_button"])
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ModuleActions(
        script: ModuleInfo,
        canUpdate: Boolean,
        hasChangeLog: Boolean,
        hasHowToUse: Boolean,
        dismiss: () -> Unit
    ) {
        val coroutineScope = rememberCoroutineScope()
        LazyColumnBottomSheet(
            onDismiss = { dismiss() }
        ) {
            val actions = remember {
                mutableMapOf<Pair<String, ImageVector>, suspend () -> Unit>().apply {
                    if (canUpdate) {
                        put(context.translation["manager.dialogs.script_action.update_module"] to Icons.Rounded.Download) {
                            coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                            context.shortToast("Updating script ${script.name}...")
                            runCatching {
                                val modulePath =
                                    context.scriptManager.getModulePath(script.name)
                                        ?: throw Exception("Module not found")
                                context.scriptManager.unloadScript(modulePath)
                                val moduleInfo = context.scriptManager.importFromUrl(
                                    script.updateUrl!!,
                                    filepath = modulePath
                                )
                                context.shortToast("Updated ${script.name} to version ${moduleInfo.version}")
                                context.database.setScriptEnabled(script.name, false)
                                withContext(context.database.executor.asCoroutineDispatcher()) {
                                    localReloadDispatcher.dispatch()
                                }
                            }.onFailure {
                                context.log.error("Failed to update module", it)
                                context.shortToast("Failed to update module. Check logs for more details")
                            }
                        }
                    }
                    if (hasHowToUse) {
                        put(context.translation["manager.dialogs.script_action.how_to_use_module"] to Icons.AutoMirrored.Rounded.Help) {
                            coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                            runCatching {
                                context.androidContext.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = script.howToUseUrl?.toUri()
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            }.onFailure {
                                context.log.error("Failed to open how to use URL", it)
                                context.shortToast("Failed to open how to use URL. Check logs for more details")
                            }
                        }
                    }
                    if (hasChangeLog) {
                        put(context.translation["manager.dialogs.script_action.view_module_changelog"] to Icons.AutoMirrored.Rounded.MenuBook) {
                            coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                            runCatching {
                                context.androidContext.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = script.changelogUrl?.toUri()
                                            ?: throw Exception("No changelog URL provided")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            }.onFailure {
                                context.log.error("Failed to open changelog", it)
                                context.shortToast("Failed to open changelog. Check logs for more details")
                            }
                        }
                    }
                    put(context.translation["manager.dialogs.script_action.edit_module"] to Icons.Rounded.Edit) {
                        coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                        runCatching {
                            val modulePath = context.scriptManager.getModulePath(script.name)!!
                            context.androidContext.startActivity(
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = context.scriptManager.getScriptsFolder()!!
                                        .findFile(modulePath)!!.uri
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                }
                            )
                        }.onFailure {
                            context.log.error("Failed to open module file", it)
                            context.shortToast("Failed to open module file. Check logs for more details")
                        }
                    }
                    put(context.translation["manager.dialogs.script_action.clear_module_data"] to Icons.Rounded.Save) {
                        coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                        runCatching {
                            context.scriptManager.getModuleDataFolder(script.name)
                                .deleteRecursively()
                            context.shortToast("Module data cleared!")
                        }.onFailure {
                            context.log.error("Failed to clear module data", it)
                            context.shortToast("Failed to clear module data. Check logs for more details")
                        }
                    }
                    put(context.translation["manager.dialogs.script_action.delete_module"] to Icons.Rounded.DeleteOutline) {
                        coroutineScope.launch { dismiss() }.invokeOnCompletion { dismiss() }
                        context.scriptManager.apply {
                            runCatching {
                                val modulePath = getModulePath(script.name)!!
                                unloadScript(modulePath)
                                getScriptsFolder()?.findFile(modulePath)?.delete()
                                localReloadDispatcher.dispatch()
                                context.shortToast("Deleted script ${script.name}!")
                            }.onFailure {
                                context.log.error("Failed to delete module", it)
                                context.shortToast("Failed to delete module. Check logs for more details")
                            }
                        }
                    }
                }.toMap()
            }
            Text(
                text = context.translation["manager.dialogs.script_action.title"],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                actions.entries.forEachIndexed { index, action ->
                    Card(
                        shape = cardShape(actions.size, index),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            modifier = Modifier
                                .clickable {
                                    context.coroutineScope.launch {
                                        action.value()
                                    }
                                }
                                .fillMaxWidth(),
                            leadingContent = {
                                Icon(
                                    imageVector = action.key.second,
                                    contentDescription = action.key.first
                                )
                            },
                            headlineContent = {
                                Text(text = action.key.first)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LabelItem(
        icon: ImageVector,
        text: String,
        containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        shape: Shape = RoundedCornerShape(500.dp),
        contentColor: Color
    ) =
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = text,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp)
                )
            }
        }

    @Composable
    fun ModuleItem(script: ModuleInfo) {
        var enabled by rememberAsyncMutableState(defaultValue = false, keys = arrayOf(script)) {
            context.database.isScriptEnabled(script.name)
        }
        var openSettings by remember(script) { mutableStateOf(false) }
        var openActions by remember { mutableStateOf(false) }

        val dispatcher = rememberAsyncUpdateDispatcher()
        val reloadCallback = remember { suspend { dispatcher.dispatch() } }
        val latestUpdate by rememberAsyncMutableState(
            defaultValue = null,
            updateDispatcher = dispatcher,
            keys = arrayOf(script)
        ) {
            context.scriptManager.checkForUpdate(script)
        }

        val shouldDisableToasts = context.config.root.scripting.disableToasts.get()

        LaunchedEffect(Unit) {
            localReloadDispatcher.addCallback(reloadCallback)
        }

        DisposableEffect(Unit) {
            onDispose {
                localReloadDispatcher.removeCallback(reloadCallback)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ElevatedCard(
                shape = cardShapeGroupedTop,
                modifier = Modifier.fillMaxWidth(),
                onClick = { routes.moduleInfo.navigate { put("name", script.name) } }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showScriptCover && !script.scriptCoverUrl.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val coverUrl = script.scriptCoverUrl
                            if (coverUrl != null && coverUrl.startsWith("data:image/")) {
                                Base64ToImage(base64String = coverUrl)
                            } else {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "Script Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            ),
                                            startY = 0.4f
                                        )
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = script.displayName ?: script.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${script.version} by ${script.author}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (!script.description.isNullOrEmpty()) {
                            Text(
                                text = script.description.let { "$it" },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        if (!script.note.isNullOrEmpty()) {
                            Text(
                                text = "Note: ${script.note}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        if (script.notice.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val rowNotices = mutableListOf<String>()
                                var hasInternalBehavior = false

                                for (notice in script.notice) {
                                    when (notice) {
                                        "unstable", "ban_risk" -> rowNotices.add(notice)
                                        "internal_behavior" -> hasInternalBehavior = true
                                        else -> {
                                            context.log.error("notice: $notice is not supported")
                                        }
                                    }
                                }

                                if (rowNotices.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        for (notice in rowNotices) {
                                            when (notice) {
                                                "unstable" -> LabelItem(
                                                    icon = Icons.Rounded.Warning,
                                                    text = "Unstable",
                                                    contentColor = Color(0xFFFFFB87)
                                                )
                                                "ban_risk" -> LabelItem(
                                                    icon = Icons.Rounded.Warning,
                                                    text = "This script may cause bans",
                                                    contentColor = Color(0xFFFF8585)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (hasInternalBehavior) {
                                    LabelItem(
                                        icon = Icons.Rounded.Warning,
                                        text = "This script may break Snapchat's internal behavior",
                                        contentColor = Color(0xFFFFFB87)
                                    )
                                }
                            }
                        }

                        latestUpdate?.let {
                            Text(
                                text = "Update available: ${it.version}",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            ElevatedCard(
                shape = cardShapeGroupedBottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (enabled) {
                            IconButton(
                                onClick = {
                                    if (!enabled) return@IconButton
                                    openSettings = !openSettings
                                },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { openActions = !openActions },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(imageVector = Icons.Rounded.Build, contentDescription = "Actions")
                        }
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { isChecked ->
                            openSettings = false
                            context.coroutineScope.launch(Dispatchers.IO) {
                                runCatching {
                                    val modulePath =
                                        context.scriptManager.getModulePath(script.name)!!
                                    context.scriptManager.unloadScript(modulePath)
                                    if (isChecked) {
                                        context.scriptManager.loadScript(modulePath)
                                        context.scriptManager.runtime.getModuleByName(script.name)
                                            ?.callFunction("module.onSnapEnhanceLoad")
                                        if (!shouldDisableToasts) {
                                            context.shortToast("Loaded script ${script.name}")
                                        }
                                    } else {
                                        if (!shouldDisableToasts) {
                                            context.shortToast("Unloaded script ${script.name}")
                                        }
                                    }

                                    context.database.setScriptEnabled(script.name, isChecked)
                                    withContext(Dispatchers.Main) {
                                        enabled = isChecked
                                    }
                                }.onFailure { throwable ->
                                    withContext(Dispatchers.Main) {
                                        enabled = !isChecked
                                    }
                                    ("Failed to ${if (isChecked) "enable" else "disable"} script. Check logs for more details").also {
                                        context.log.error(it, throwable)
                                        context.shortToast(it)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        if (openSettings) {
            ModuleSettings(
                script = script
            ) { openSettings = false }
        }

        if (openActions) {
            ModuleActions(
                script = script,
                canUpdate = latestUpdate != null,
                hasChangeLog = script.changelogUrl != null,
                hasHowToUse = script.howToUseUrl != null,
            ) { openActions = false }
        }
    }

    override val floatingActionButton: @Composable () -> Unit = {
        var showImportBottomSheet by remember { mutableStateOf(false) }
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        if (showImportBottomSheet) {
            ImportRemoteScript {
                showImportBottomSheet = false
            }
        }
        val scriptingFolder = remember(reloadTrigger) {
            context.scriptManager.getScriptsFolder()
        }
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

        when (selectedIndex) {
            0 -> {
                if (scriptingFolder != null) {
                    FloatingActionButtonMenu(
                        modifier = Modifier.offset(x = 18.dp, y = 18.dp),
                        expanded = fabMenuExpanded,
                        button = {
                            ToggleFloatingActionButton(
                                modifier = Modifier.semantics {
                                    traversalIndex = -1f
                                    stateDescription =
                                        if (fabMenuExpanded) "Expanded" else "Collapsed"
                                    contentDescription = "null"
                                },
                                checked = fabMenuExpanded,
                                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                                containerColor = { primaryColor },
                            ) {
                                val imageVector by remember {
                                    derivedStateOf {
                                        if (checkedProgress > 0.5f) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.Launch
                                    }
                                }
                                Icon(
                                    painter = rememberVectorPainter(imageVector),
                                    contentDescription = null,
                                    modifier = Modifier.animateIcon(
                                        checkedProgress = { checkedProgress },
                                        color = { onPrimaryColor }
                                    )
                                )
                            }
                        },
                    ) {
                        FloatingActionButtonMenuItem(
                            onClick = {
                                context.scriptManager.getScriptsFolder()?.let {
                                    context.androidContext.openLink(it.uri.toString())
                                }
                                fabMenuExpanded = false
                            },
                            icon = {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = "Folder"
                                )
                            },
                            text = { Text(text = translation["open_scripts_folder_button"]) },
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
                        )

                        FloatingActionButtonMenuItem(
                            onClick = {
                                if (context.scriptManager.getScriptsFolder() != null) {
                                    showImportBottomSheet = true
                                }
                                fabMenuExpanded = false
                            },
                            icon = { Icon(Icons.Rounded.Link, contentDescription = "Link") },
                            text = { Text(text = translation["import_from_url_button"]) },
                            containerColor = primaryColor,
                            contentColor = onPrimaryColor
                        )
                    }
                }
            }

            1 -> {
                ExtendedFloatingActionButton(
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                    onClick = { routes.manageRepos.navigate() },
                    icon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    text = { Text(context.translation["manager.sections.theming.manage_repositories_button"]) },
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor
                )
            }
        }
    }

    @Composable
    private fun ModuleSettings(
        script: ModuleInfo,
        dismiss: () -> Unit
    ) {
        val settingsInterface = remember {
            val module =
                context.scriptManager.runtime.getModuleByName(script.name) ?: return@remember null
            (module.getBinding(InterfaceManager::class))?.buildInterface(EnumScriptInterface.SETTINGS)
        }

        LazyColumnBottomSheet(
            onDismiss = { dismiss() }
        ) {
            if (settingsInterface == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = context.translation["manager.dialogs.script_settings.no_settings_found"],
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                ScriptInterface(interfaceBuilder = settingsInterface)
            }
        }
    }

    @Composable
    private fun InstalledScripts() {
        val scriptingFolder by rememberAsyncMutableState(
            defaultValue = null,
            updateDispatcher = localReloadDispatcher
        ) {
            context.scriptManager.getScriptsFolder()
        }

        val scriptModules by rememberAsyncMutableState(
            defaultValue = emptyList(),
            updateDispatcher = localReloadDispatcher
        ) {
            context.scriptManager.sync()
            context.scriptManager.getSyncedModules()
        }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var refreshing by remember { mutableStateOf(false) }

        LaunchedEffect(key1 = scriptModules) {
            listState.scrollToItem(0)
        }

        LaunchedEffect(Unit) {
            refreshing = true
            withContext(Dispatchers.IO) {
                localReloadDispatcher.dispatch()
                refreshing = false
            }
        }

        val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
            refreshing = true
            coroutineScope.launch(Dispatchers.IO) {
                localReloadDispatcher.dispatch()
                refreshing = false
            }
        })

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState),
                contentPadding = lazyColumnContentPadding(
                    staticVertical = 10.dp,
                    hasSubAction = true,
                    hasFAB = true
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (scriptingFolder == null || scriptModules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (scriptingFolder == null && !refreshing) {
                                    ElevatedCard(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .size(200.dp),
                                        shape = MaterialShapes.Pill.toShape()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DataObject,
                                                contentDescription = null,
                                                modifier = Modifier.size(100.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = translation["set_up_script_folder"],
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            activityLauncherHelper.chooseFolder {
                                                context.config.root.scripting.moduleFolder.set(it)
                                                context.config.writeConfig()
                                                reloadTrigger++
                                                coroutineScope.launch {
                                                    localReloadDispatcher.dispatch()
                                                }
                                            }
                                        },
                                        shapes = ButtonDefaults.shapes()
                                    ) {
                                        Text(text = translation["select_folder_button"])
                                    }
                                } else if (scriptModules.isEmpty()) {
                                    ElevatedCard(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .size(200.dp),
                                        shape = MaterialShapes.Pill.toShape()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DataObject,
                                                contentDescription = null,
                                                modifier = Modifier.size(100.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = translation["scripts_empty"],
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Text(
                                        text = translation["scripts_empty_desc"],
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = scriptModules,
                        key = { _, script -> script.hashCode() }
                    ) { index, script ->
                        ModuleItem(script)
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter).componentPadding(
                    staticVertical = 0.dp,
                    staticHorizontal = 0.dp,
                    hasSubAction = true
                )
            )

            var scriptingWarning by remember {
                mutableStateOf(context.sharedPreferences.run {
                    getBoolean("scripting_warning", true).also {
                        edit().putBoolean("scripting_warning", false).apply()
                    }
                })
            }

            if (scriptingWarning) {
                var timeout by remember { mutableIntStateOf(10) }

                LaunchedEffect(Unit) {
                    while (timeout > 0) {
                        delay(1000)
                        timeout--
                    }
                }

                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { targetValue ->
                        timeout == 0 || targetValue != SheetValue.Hidden
                    }
                )

                LazyColumnBottomSheet(
                    sheetState = sheetState,
                    sheetGesturesEnabled = true,
                    properties = ModalBottomSheetProperties(
                        shouldDismissOnBackPress = false,
                        shouldDismissOnClickOutside = false
                    ),
                    onDismiss = {
                        if (timeout == 0) {
                            scriptingWarning = false
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.translation["manager.dialogs.scripting_warning.title"],
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = context.translation["manager.dialogs.scripting_warning.content"],
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        TextButton(
                            onClick = { scriptingWarning = false },
                            enabled = timeout == 0,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = "OK " + if (timeout > 0) "($timeout)" else "")
                        }
                    }
                }
            }
        }
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        IconButton(
            onClick = {
                context.androidContext.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = "https://git.seextended.org/bocajthomas/SE-Extended/wiki/Scripting-Documentation".toUri()
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.LibraryBooks,
                contentDescription = "Documentation"
            )
        }
    }

    override val topBarSubActions: @Composable (RowScope.() -> Unit) = {
        val getSelectedIndex = selectedIndex

        FlowRow(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            scriptOptions.forEachIndexed { index, label ->
                ToggleButton(
                    checked = getSelectedIndex == index,
                    onCheckedChange = { selectedIndex = index },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            scriptOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    modifier = Modifier.semantics { role = Role.RadioButton }
                        .weight(1f),
                ) {
                    Text(label)
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val getCurrentSelectedIndex = selectedIndex
        val pagerState = rememberPagerState(
            initialPage = getCurrentSelectedIndex,
            pageCount = { scriptOptions.size }
        )

        LaunchedEffect(getCurrentSelectedIndex) {
            if (getCurrentSelectedIndex != pagerState.currentPage) {
                pagerState.animateScrollToPage(getCurrentSelectedIndex)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (selectedIndex != pagerState.currentPage) {
                selectedIndex = pagerState.currentPage
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> InstalledScripts()
                1 -> ScriptCatalog(this@ScriptingRootSection)
            }
        }
    }
}