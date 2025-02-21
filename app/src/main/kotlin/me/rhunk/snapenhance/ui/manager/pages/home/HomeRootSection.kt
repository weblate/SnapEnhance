package me.rhunk.snapenhance.ui.manager.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.action.EnumQuickActions
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.action.EnumAction
import me.rhunk.snapenhance.common.ui.TopBarActionButton
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableState
import me.rhunk.snapenhance.common.ui.rememberAsyncMutableStateList
import me.rhunk.snapenhance.common.util.ktx.openLink
import me.rhunk.snapenhance.core.ui.Snapenhance
import me.rhunk.snapenhance.storage.getQuickTiles
import me.rhunk.snapenhance.storage.setQuickTiles
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.data.Updater
import me.rhunk.snapenhance.ui.util.ActivityLauncherHelper
import java.text.DateFormat

class HomeRootSection : Routes.Route() {
    companion object {
        val cardMargin = 10.dp
    }

    private lateinit var activityLauncherHelper: ActivityLauncherHelper

    private val cards by lazy {
        EnumQuickActions.entries.map {
            (context.translation["actions.${it.key}.name"] to it.icon) to it.action
        }.associate {
            it.first to it.second
        }.toMutableMap().apply {
            EnumAction.entries.forEach { action ->
                this[context.translation["actions.${action.key}.name"] to action.icon] = {
                    context.launchActionIntent(action)
                }
            }
        }
    }

    @Composable
    private fun InfoCard(
        content: @Composable ColumnScope.() -> Unit,
    ) {
        OutlinedCard(
            modifier = Modifier
                .padding(start = cardMargin, end = cardMargin)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp)
            ) {
                content()
            }
        }
    }

    @Composable
    fun ExternalLinkIcon(
        modifier: Modifier = Modifier,
        size: Dp = 32.dp,
        imageVector: ImageVector,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .then(modifier)
        )
    }

    override val title: @Composable (() -> Unit)? = {}

    override val init: () -> Unit = {
        activityLauncherHelper = ActivityLauncherHelper(context.activity!!)
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        TopBarActionButton(
            onClick = {
                routes.homeLogs.navigate()
            },
            icon = Icons.Filled.BugReport,
            text = context.translation["manager.routes.home_logs"]
        )
        Spacer(modifier = Modifier.width(8.dp))
        TopBarActionButton(
            onClick = {
                routes.settings.navigate()
            },
            icon = Icons.Filled.Settings,
            text = context.translation["manager.routes.home_settings"]
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val avenirNext = remember {
            FontFamily(
                Font(R.font.avenir_next_medium, FontWeight.Medium)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Snapenhance, contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = translation.format(
                    "version_title",
                    "versionName" to BuildConfig.VERSION_NAME
                ),
                fontSize = 14.sp,
                fontFamily = avenirNext,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    15.dp, Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 5.dp)
            ) {
                ExternalLinkIcon(
                    modifier = Modifier.clickable {
                        context.androidContext.openLink("https://t.me/snapenhance")
                    },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_telegram),
                )

                ExternalLinkIcon(
                    modifier = Modifier.clickable {
                        context.androidContext.openLink("https://github.com/rhunk/SnapEnhance")
                    },
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_github),
                )

                ExternalLinkIcon(
                    modifier = Modifier.offset(x = (-3).dp).clickable {
                        context.androidContext.openLink("https://github.com/rhunk/SnapEnhance/wiki")
                    },
                    size = 40.dp,
                    imageVector = Icons.AutoMirrored.Default.Help,
                )
            }

            val selectedTiles = rememberAsyncMutableStateList(defaultValue = listOf()) {
                context.database.getQuickTiles()
            }

            val latestUpdate by rememberAsyncMutableState(defaultValue = null) { Updater.latestRelease }

            if (latestUpdate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = translation["update_title"],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                fontSize = 12.sp,
                                text = translation.format(
                                    "update_content",
                                    "version" to (latestUpdate?.versionName ?: "unknown")
                                ),
                                lineHeight = 20.sp,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Button(
                            modifier = Modifier.height(40.dp),
                            onClick = {
                                latestUpdate?.releaseUrl?.let { context.androidContext.openLink(it) }
                            }
                        ) {
                            Text(text = translation["update_button"])
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoCard {
                    Text(
                        text = translation["debug_build_summary_title"],
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val buildSummary = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Light
                            )
                        ) {
                            append(
                                remember {
                                    translation.format(
                                        "debug_build_summary_content",
                                        "versionName" to BuildConfig.VERSION_NAME,
                                        "versionCode" to BuildConfig.VERSION_CODE.toString(),
                                    )
                                }
                            )
                            append(" - ")
                        }
                        withLink(
                            LinkAnnotation.Clickable(
                                "git_hash",
                                linkInteractionListener = {
                                    context.androidContext.openLink("https://github.com/rhunk/SnapEnhance/commit/${BuildConfig.GIT_HASH}")
                                }
                            )
                        ) {
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append(BuildConfig.GIT_HASH.substring(0, 7))
                            }
                        }
                    }
                    Text(
                        text = buildSummary
                    )
                    Text(
                        fontSize = 12.sp,
                        text = remember {
                            translation.format(
                                "debug_build_summary_date",
                                "date" to DateFormat.getDateTimeInstance()
                                    .format(BuildConfig.BUILD_TIMESTAMP),
                                "days" to ((System.currentTimeMillis() - BuildConfig.BUILD_TIMESTAMP) / 86400000).toInt()
                                    .toString()
                            )
                        },
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            var showQuickActionsMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    translation["quick_actions_title"], fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(
                        onClick = { showQuickActionsMenu = !showQuickActionsMenu },
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showQuickActionsMenu,
                        onDismissRequest = { showQuickActionsMenu = false }
                    ) {
                        cards.forEach { (card, _) ->
                            fun toggle(state: Boolean? = null) {
                                if (state?.let { !it } ?: selectedTiles.contains(card.first)) {
                                    selectedTiles.remove(card.first)
                                } else {
                                    selectedTiles.add(0, card.first)
                                }
                                context.coroutineScope.launch {
                                    context.database.setQuickTiles(selectedTiles)
                                }
                            }

                            DropdownMenuItem(onClick = { toggle() }, text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(all = 5.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedTiles.contains(card.first),
                                        onCheckedChange = {
                                            toggle(it)
                                        }
                                    )
                                    Text(text = card.first)
                                }
                            })
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier
                    .padding(all = cardMargin)
                    .fillMaxWidth(),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val tileHeight = LocalDensity.current.run {
                    remember { (context.androidContext.resources.displayMetrics.widthPixels / 3).toDp() - cardMargin / 2 }
                }

                remember(selectedTiles.size, context.translation.loadedLocale) {
                    selectedTiles.mapNotNull {
                        cards.entries.find { entry -> entry.key.first == it }
                    }
                }.forEach { (card, action) ->
                    ElevatedCard(
                        modifier = Modifier
                            .height(tileHeight)
                            .weight(1f)
                            .padding(all = 6.dp),
                        onClick = { action(routes) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(all = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Icon(
                                imageVector = card.second, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(50.dp)
                            )
                            Text(
                                text = card.first,
                                lineHeight = 16.sp,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
