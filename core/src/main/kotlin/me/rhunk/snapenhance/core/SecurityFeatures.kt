package me.rhunk.snapenhance.core

import android.system.Os
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.bridge.toWrapper
import me.rhunk.snapenhance.common.config.MOD_DETECTION_VERSION_CHECK
import me.rhunk.snapenhance.common.config.VersionRequirement
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.ui.CustomComposable
import me.rhunk.snapenhance.core.util.ktx.setObjectField
import java.io.FileDescriptor
import kotlin.text.isNotEmpty

class SecurityFeatures(
    private val context: ModContext
) {
    private fun transact(option: Int, option2: Long) = runCatching { Os.prctl(option, option2, 0, 0, 0) }.getOrNull()

    private val token by lazy {
        transact(0, 0)
    }

    private fun getStatus() = token?.run {
        transact(this, 0)?.toString(2)?.padStart(32, '0')?.count { it == '1' }
    }

    fun init(): Boolean {
        val snapchatVersionCode = context.androidContext.packageManager?.getPackageInfo(context.androidContext.packageName, 0)?.longVersionCode ?: throw IllegalStateException("Failed to get version code")
        val shouldUseSafeMode = MOD_DETECTION_VERSION_CHECK.checkVersion(snapchatVersionCode)?.second == VersionRequirement.OLDER_REQUIRED // true if version is >12.81.0.44
        val debugDisable = context.bridgeClient.getDebugProp("disable_sif_prod", "false") == "true"

        context.config.experimental.nativeHooks.customSharedLibrary.get().takeIf { it.isNotEmpty() }?.let {
            runCatching {
                context.native.loadSharedLibrary(
                    context.fileHandlerManager.getFileHandle(FileHandleScope.USER_IMPORT.key, it).toWrapper().readBytes()
                )
                context.log.verbose("loaded custom shared library")
            }.onFailure {
                context.log.error("Failed to load custom shared library", it)
                return true
            }
        }

        if (!debugDisable) {
            runCatching {
                context.native.loadSharedLibrary(
                    context.fileHandlerManager.getFileHandle(FileHandleScope.INTERNAL.key, InternalFileHandleType.SIF.key)
                        .toWrapper()
                        .readBytes()
                        .takeIf {
                            it.isNotEmpty()
                        } ?: throw IllegalStateException("buffer is empty")
                )
                context.log.verbose("loaded sif")
            }.onFailure {
                context.log.error("Failed to load sif", it)
                return true
            }
        } else {
            context.log.warn("sif is disabled")
        }

        token // pre init token

        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (!event.uri.contains("/Login")) return@subscribe

            // intercept login response
            event.addResponseCallback {
                val response = ProtoReader(buffer)
                val isBlocked = when {
                    event.uri.contains("TLv") -> response.getVarInt(1) == 14L
                    else -> response.getVarInt(1) == 16L
                }

                val errorDataIndex = when {
                    response.contains(11) -> 11
                    response.contains(10) -> 10
                    response.contains(8) -> 8
                    else -> return@addResponseCallback
                }

                if (isBlocked) {
                    val status = transact(token ?: return@addResponseCallback, 1)
                        ?.takeIf { it != 0 }
                        ?.let {
                            val buffer = ByteArray(8192)
                            val fd = FileDescriptor().apply {
                                setObjectField("descriptor", it)
                            }
                            val read = Os.read(fd, buffer, 0, buffer.size)
                            Os.close(fd)
                            buffer.copyOfRange(0, read).decodeToString()
                        } ?: return@addResponseCallback

                    buffer = ProtoEditor(buffer).apply {
                        edit(errorDataIndex) {
                            remove(1)
                            addString(1, status)
                        }
                    }.toByteArray()
                }
            }
        }

        val status = getStatus()
        val safeMode = shouldUseSafeMode && (status == null || status < 2)

        if (status != null && status >= 2) {
            lateinit var composable: CustomComposable
            composable = {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF85A947))
                }

                LaunchedEffect(Unit) {
                    delay(2500)
                    context.inAppOverlay.removeCustomComposable(composable)
                }
            }
            context.inAppOverlay.addCustomComposable(composable)
        }

        if (safeMode && !debugDisable) {
            context.features.addActivityCreateListener {
                context.inAppOverlay.showStatusToast(
                    icon = Icons.Filled.NotInterested,
                    text = "SnapEnhance is not compatible with this version of Snapchat without SIF and will result in a ban.\nUse Snapchat ${MOD_DETECTION_VERSION_CHECK.maxVersion?.first ?: "0.0.0"} or older to avoid detections or use test accounts.",
                    durationMs = 10000,
                    maxLines = 6
                )
            }
        }

        return safeMode
    }
}