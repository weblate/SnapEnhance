@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.ui.manager.Routes

class DebugTestingUI: Routes.Route() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val context = LocalContext.current
        val vibrator = remember { ContextCompat.getSystemService(context, Vibrator::class.java) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text("Haptics")

                Button(
                    onClick =  {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = "EFFECT_TICK")
                }

                Button(
                    onClick = {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = "EFFECT_CLICK")
                }

                Button(
                    onClick = {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = "EFFECT_DOUBLE_CLICK")
                }

                Button(
                    onClick = {
                        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(text = "EFFECT_HEAVY_CLICK")
                }
            }
        }
    }
}