@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.scripting

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.storage.getRepositories
import me.rhunk.snapenhance.ui.util.pullrefresh.PullRefreshIndicator
import me.rhunk.snapenhance.ui.util.pullrefresh.pullRefresh
import me.rhunk.snapenhance.ui.util.pullrefresh.rememberPullRefreshState
import okhttp3.Request

private val cachedRepoIndexes = mutableStateMapOf<String, RepositoryIndex>()
private val cacheReloadDispatcher = AsyncUpdateDispatcher()

@Composable
fun ScriptCatalog(root: ScriptingRootSection) {
    val context = remember { root.context }
    val coroutineScope = rememberCoroutineScope()
    val installedScripts by rememberAsyncMutableState(
        defaultValue = context.scriptManager.getSyncedModules(),
        updateDispatcher = root.localReloadDispatcher
    ) {
        context.scriptManager.sync()
        context.scriptManager.getSyncedModules()
    }

    fun fetchRepoIndexes(): Map<String, RepositoryIndex> {
        val indexes = mutableMapOf<String, RepositoryIndex>()
        context.database.getRepositories().forEach { rootUri ->
            val indexUri = rootUri.toUri().buildUpon().appendPath("index.json").build()
            runCatching {
                root.okHttpClient.newCall(Request.Builder().url(indexUri.toString()).build()).execute().use { response ->
                    if (!response.isSuccessful) return@forEach
                    indexes[rootUri] = context.gson.fromJson(response.body?.charStream(), RepositoryIndex::class.java)
                }
            }
        }
        return indexes
    }

    suspend fun installScript(scriptUri: Uri, filename: String) {
        root.okHttpClient.newCall(
            Request.Builder().url(scriptUri.toString()).build()
        ).execute().use { response ->
            if (!response.isSuccessful) return
            val scriptContent = response.body?.string()
            val scriptsFolder = context.scriptManager.getScriptsFolder() ?: return

            withContext(Dispatchers.IO) {
                val file = scriptsFolder.findFile(filename) ?: scriptsFolder.createFile("application/javascript", filename)
                context.androidContext.contentResolver.openOutputStream(file!!.uri)?.use { it.write(scriptContent?.toByteArray()) }
            }
            context.scriptManager.sync()
            root.localReloadDispatcher.dispatch()
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun refreshCachedIndexes() {
        isRefreshing = true
        withContext(Dispatchers.IO) {
            fetchRepoIndexes().let {
                synchronized(cachedRepoIndexes) {
                    cachedRepoIndexes.clear()
                    cachedRepoIndexes += it
                }
                cacheReloadDispatcher.dispatch()
            }
        }
        delay(500)
        isRefreshing = false
    }

    val remoteScripts by rememberAsyncMutableState(defaultValue = emptyList(), updateDispatcher = cacheReloadDispatcher) {
        cachedRepoIndexes.entries.flatMap { entry -> entry.value.scripts.map { script -> entry.key to script } }
    }

    LaunchedEffect(Unit) {
        if (cachedRepoIndexes.isEmpty()) refreshCachedIndexes()
    }

    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = { coroutineScope.launch { refreshCachedIndexes() } })

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp, hasSubAction = true, hasFAB = true),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                if (remoteScripts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ElevatedCard(
                                modifier = Modifier.padding(16.dp).size(200.dp),
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
                                text = context.translation["manager.sections.scripts.script_catalog_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                text = context.translation["manager.sections.scripts.script_catalog_empty_desc"],
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            itemsIndexed(remoteScripts, key = { _, item -> item.first + item.second.name }) { index, (repoUrl, scriptManifest) ->
                val scriptUri = remember(repoUrl, scriptManifest) {
                    repoUrl.toUri().buildUpon().appendPath(scriptManifest.filepath ?: "${scriptManifest.name}.js").build()
                }

                var isInstalledOnDisk by remember(scriptManifest) { mutableStateOf(false) }

                LaunchedEffect(scriptManifest, installedScripts) {
                    withContext(Dispatchers.IO) {
                        val folder = context.scriptManager.getScriptsFolder()
                        val filename = scriptManifest.filepath ?: "${scriptManifest.name}.js"
                        isInstalledOnDisk = folder?.findFile(filename)?.exists() == true
                    }
                }
                val isInstalled = isInstalledOnDisk || installedScripts.any { it.name == scriptManifest.name }

                val hasUpdate by rememberAsyncMutableState(
                    defaultValue = false,
                    keys = arrayOf(scriptManifest, installedScripts)
                ) {
                    installedScripts.find { it.name == scriptManifest.name }?.let { installedScript ->
                        val installedVer = installedScript.version?.trim() ?: "unknown"
                        val manifestVer = scriptManifest.version?.trim() ?: "unknown"

                        installedVer != manifestVer
                    } ?: false
                }

                var isInstalling by remember { mutableStateOf(false) }

                ElevatedCard(
                    onClick = {},
                    shape = cardShape(remoteScripts.size, index)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.DataObject, contentDescription = null, modifier = Modifier.padding(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = scriptManifest.displayName ?: scriptManifest.name,
                                maxLines = 1,
                                fontSize = 16.sp,
                                lineHeight = 10.sp,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                            scriptManifest.author?.let {
                                Text(
                                    text = "by $it",
                                    maxLines = 1,
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Light,
                                    overflow = TextOverflow.Visible,
                                )
                            }
                            scriptManifest.description?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    lineHeight = 16.sp,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (hasUpdate) {
                                Text(
                                    text = "Version ${scriptManifest.version} available",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isInstalling) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Button(
                                    enabled = !isInstalled || hasUpdate,
                                    onClick = {
                                        isInstalling = true
                                        context.coroutineScope.launch(Dispatchers.IO) {
                                            runCatching {
                                                installScript(scriptUri, scriptManifest.filepath ?: "${scriptManifest.name}.js")
                                            }.onFailure {
                                                withContext(Dispatchers.Main) { context.shortToast("Failed to install") }
                                            }
                                            withContext(Dispatchers.Main) {
                                                isInstalling = false
                                            }
                                        }
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    if (hasUpdate) {
                                        Text(context.translation["manager.sections.theming.update_theme_button"])
                                    } else {
                                        Text(if (isInstalled) context.translation["manager.sections.theming.installed_theme_button"] else context.translation["manager.sections.theming.install_theme_button"])
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter).componentPadding(staticVertical = 0.dp, staticHorizontal = 0.dp, hasSubAction = true)
        )
    }
}