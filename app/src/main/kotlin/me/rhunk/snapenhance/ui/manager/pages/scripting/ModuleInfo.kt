package me.rhunk.snapenhance.ui.manager.pages.scripting

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import coil.compose.AsyncImage
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.cardShapeSingle
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import me.rhunk.snapenhance.ui.util.Base64ToImage
import java.text.DateFormat

class ModuleInfo : Routes.Route() {
    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    // override val shouldHideTopAppBar: Boolean = true

    fun getSystemFileMetadata(moduleName: String): Pair<Long, Long>? {
        val modulePath = context.scriptManager.getModulePath(moduleName) ?: return null
        val file = context.scriptManager.getScriptsFolder()?.findFile(modulePath) ?: return null

        return Pair(file.length(), file.lastModified())
    }

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }
    override val floatingActionButton: @Composable () -> Unit = {}
    override val content: @Composable (NavBackStackEntry) -> Unit = { backStackEntry ->
        val moduleName = backStackEntry.arguments?.getString("name")
        val script = remember(moduleName) {
            context.scriptManager.getSyncedModules().find { it.name == moduleName }
        }
        val metadata = remember(moduleName) { moduleName?.let { getSystemFileMetadata(it) } }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                script?.let {
                    val cardItems = buildList {
                        if (it.license != null) {
                            add("License" to it.license!!)
                        }
                        add("Version" to "v${it.version}")
                        metadata?.let { (fileSize, lastModified) ->
                            add("Last Updated" to DateFormat.getDateInstance().format(lastModified))
                            add("File Size" to Formatter.formatFileSize(context.androidContext, fileSize))
                        }
                    }

                    val groupSize = cardItems.size

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ElevatedCard(
                            modifier = Modifier.size(90.dp),
                            shape = cardShapeSingle,
                        ) {
                            val iconUrl = script.scriptIconUrl
                            if (iconUrl != null && iconUrl.startsWith("data:image/")) {
                                Base64ToImage(base64String = iconUrl)
                            } else {
                                AsyncImage(
                                    model = iconUrl,
                                    fallback = painterResource(id = R.drawable.launcher_icon_monochrome),
                                    error = painterResource(id = R.drawable.launcher_icon_monochrome),
                                    contentDescription = "Script Icon",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Text(
                                text = it.displayName ?: it.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "By ${it.author ?: "Unknown"}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        cardItems.forEachIndexed { index, (title, value) ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = cardShape(groupSize = groupSize, index = index)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(15.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    Text(
                                        text = value,
                                        modifier = Modifier.padding(end = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}