@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.data.theme.ThemeMode
import me.rhunk.snapenhance.common.data.theme.updateSystemBars
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.cardShapeSingle
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState
import me.rhunk.snapenhance.common.ui.theme.Themes
import me.rhunk.snapenhance.ui.components.PalettePreview
import me.rhunk.snapenhance.ui.manager.Routes

class HomeAppTheme: Routes.Route() {
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val localContext = LocalContext.current
        val preferences = localContext.getSharedPreferences("selected_theme", Context.MODE_PRIVATE)
        var selectedPalette by preferences.rememberMutableStringPreferenceState(
            key = "theme_name",
            defaultValue = Themes.getPalettes(localContext).first().key
        )

        val usePureDarkModePref = preferences.rememberMutableBooleanPreferenceState(
            key = "appearance_use_pure_dark_mode",
            defaultValue = false
        )
        val usePureDarkMode by usePureDarkModePref

        val allPalettes = Themes.getPalettes(localContext)
        var selectedThemeMode by preferences.rememberMutableStringPreferenceState(
            key = "theme_mode",
            defaultValue = ThemeMode.SYSTEM.name
        )
        val themeOptions = ThemeMode.entries.map { it.label }
        val getSelectedIndex = themeOptions.indexOf(ThemeMode.valueOf(selectedThemeMode).label)
        val setSelectedIndex: (Int) -> Unit = { index ->
            selectedThemeMode = ThemeMode.entries[index].name
        }
        val currentThemeMode = ThemeMode.valueOf(selectedThemeMode)
        LaunchedEffect(currentThemeMode) {
            val activity = localContext as? ComponentActivity
            activity?.let {
                updateSystemBars(it, currentThemeMode)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(
                staticHorizontal = 0.dp,
                staticVertical = 10.dp
            ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text(
                    text = translation["theme_palettes_title"],
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    items(allPalettes) { theme ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(125.dp)
                        ) {
                            PalettePreview(
                                theme = theme,
                                isSelected = theme.key == selectedPalette,
                                selectedThemeMode = currentThemeMode,
                                onThemeSelected = { selectedTheme ->
                                    selectedPalette = selectedTheme.key
                                }
                            )
                            Text(
                                context.translation["manager.pallets.${theme.key}"],
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Text(
                    text = translation["theme_mode_title"],
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                val showPureBlackDarkMode = selectedThemeMode == ThemeMode.DARK.name || selectedThemeMode == ThemeMode.SYSTEM.name && isSystemInDarkTheme()

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = if (showPureBlackDarkMode) { cardShapeGroupedTop } else { cardShapeSingle },
                    ) {
                        FlowRow(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            themeOptions.forEachIndexed { index, label ->
                                ToggleButton(
                                    checked = getSelectedIndex == index,
                                    onCheckedChange = { setSelectedIndex(index) },
                                    shapes =
                                        when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    modifier = Modifier.semantics {
                                        role = Role.RadioButton
                                    }.weight(1f),
                                ) {
                                    Text(translation[label])
                                }
                            }
                        }
                    }
                    if (showPureBlackDarkMode) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = cardShapeGroupedBottom,
                        ) {
                            Row(
                                modifier = Modifier.clickable { usePureDarkModePref.value = !usePureDarkModePref.value }.padding(horizontal = 15.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = translation["pure_black_dark_mode_title"],
                                    modifier = Modifier.padding(start = 5.dp).weight(1f)
                                )

                                Switch(
                                    checked = usePureDarkMode,
                                    onCheckedChange = { newState ->
                                        usePureDarkModePref.value = newState
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}