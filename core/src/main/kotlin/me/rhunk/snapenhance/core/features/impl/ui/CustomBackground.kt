/*
 * Copyright (C) 2026 bocajthomas
 * SPDX-License-Identifier: GPL-3.0
*/

package me.rhunk.snapenhance.core.features.impl.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.data.FileType
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getFileHandleLocalPath
import me.rhunk.snapenhance.mapper.impl.CustomBackgroundMapper
import java.io.File

class CustomBackground : Feature("Custom Background") {
    private var decodedBitmap: Bitmap? = null
    private val videoInstalledTag = 0x53454247

    // TODO: move this to util
    private fun videoView(path: String): TextureView {
        val textureView = TextureView(context.androidContext).apply {
            isClickable = false
            isFocusable = false
        }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            private var player: MediaPlayer? = null
            private var videoWidth = 0
            private var videoHeight = 0

            private fun updateTransform(viewWidth: Int, viewHeight: Int) {
                if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return

                val scale = maxOf(
                    viewWidth.toFloat() / videoWidth,
                    viewHeight.toFloat() / videoHeight
                )

                val scaledWidth = videoWidth * scale
                val scaledHeight = videoHeight * scale
                val matrix = Matrix()

                matrix.setScale(
                    scaledWidth / viewWidth,
                    scaledHeight / viewHeight,
                    viewWidth / 2f,
                    viewHeight / 2f
                )

                textureView.setTransform(matrix)
            }

            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val surface = Surface(surfaceTexture)
                val mediaPlayer = MediaPlayer()

                player = mediaPlayer

                mediaPlayer.setDataSource(path)
                mediaPlayer.setSurface(surface)
                mediaPlayer.isLooping = true
                mediaPlayer.setVolume(0f, 0f)

                mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                    videoWidth = width
                    videoHeight = height
                    updateTransform(textureView.width, textureView.height)
                }

                mediaPlayer.setOnPreparedListener {
                    videoWidth = it.videoWidth
                    videoHeight = it.videoHeight
                    updateTransform(textureView.width, textureView.height)
                    it.start()
                }

                mediaPlayer.prepareAsync()
                surface.release()
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                updateTransform(width, height)
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                player?.release()
                player = null
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }

        return textureView
    }

    override fun init() {
        val customBackground = context.config.userInterface.customBackgrounds.getNullable()?.takeIf { it.isNotBlank() } ?: return
        val backgroundPath = runCatching {
            context.fileHandlerManager.getFileHandleLocalPath(
                context,
                FileHandleScope.USER_IMPORT,
                customBackground,
                "custom_background"
            )
        }.onFailure {
            context.log.error("Failed to get custom background", it)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return

        val fileType = FileType.fromFile(File(backgroundPath))

        context.mappings.useMapper(CustomBackgroundMapper::class) {
            val backgroundProviderClass = backgroundProviderClass.get() ?: return@useMapper
            val headerBackgroundMethod = headerBackgroundMethod.get() ?: return@useMapper
            val contentBackgroundMethod = contentBackgroundMethod.get() ?: return@useMapper

            when {
                fileType.isImage -> {
                    setBitmapBackground(
                        backgroundPath,
                        backgroundProviderClass,
                        headerBackgroundMethod,
                        contentBackgroundMethod
                    )
                }

                fileType.isVideo -> {
                    val pullToRefreshFragmentClass = pullToRefreshFragmentClass.get() ?: return@useMapper
                    val setupMethod = setupMethod.get() ?: return@useMapper
                    val staticBackgroundMethods = staticBackgroundMethods.get()?.values?.filterNotNull().orEmpty()

                    setMediaPlayerBackground(
                        backgroundPath,
                        backgroundProviderClass,
                        staticBackgroundMethods,
                        headerBackgroundMethod,
                        contentBackgroundMethod,
                        pullToRefreshFragmentClass,
                        setupMethod
                    )
                }
            }
        }
    }

    private fun setBitmapBackground(
        backgroundPath: String,
        backgroundProviderClass: Class<*>,
        headerBackgroundMethod: String,
        contentBackgroundMethod: String
    ) {
        decodedBitmap = runCatching {
            BitmapFactory.decodeFile(backgroundPath)
        }.onFailure {
            context.log.error("Failed to decode custom background", it)
        }.getOrNull()

        backgroundProviderClass.hook(headerBackgroundMethod, HookStage.BEFORE) { param ->
            val bitmap = decodedBitmap ?: return@hook
            val headerHeight = param.arg<Int>(0)
            val contentHeight = param.arg<Int>(1)
            val totalHeight = headerHeight + contentHeight
            if (totalHeight <= 0) return@hook

            val cropHeight = (bitmap.height.toLong() * headerHeight / totalHeight).toInt().coerceIn(1, bitmap.height)

            param.setResult(
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, cropHeight).toDrawable(context.androidContext.resources)
            )
        }

        backgroundProviderClass.hook(contentBackgroundMethod, HookStage.BEFORE) { param ->
            val bitmap = decodedBitmap ?: return@hook
            val headerHeight = param.arg<Int>(0)
            val contentHeight = param.arg<Int>(1)
            val totalHeight = headerHeight + contentHeight
            if (totalHeight <= 0) return@hook

            val split = (bitmap.height.toLong() * headerHeight / totalHeight).toInt().coerceIn(0, bitmap.height - 1)

            param.setResult(
                Bitmap.createBitmap(bitmap, 0, split, bitmap.width, bitmap.height - split).toDrawable(context.androidContext.resources)
            )
        }
    }

    private fun setMediaPlayerBackground(
        backgroundPath: String,
        backgroundProviderClass: Class<*>,
        staticBackgroundMethods: Collection<String>,
        headerBackgroundMethod: String,
        contentBackgroundMethod: String,
        pullToRefreshFragmentClass: Class<*>,
        setupMethod: String
    ) {
        (staticBackgroundMethods + headerBackgroundMethod + contentBackgroundMethod).distinct().forEach { method ->
            backgroundProviderClass.hook(method, HookStage.BEFORE) { param ->
                param.setResult(Color.TRANSPARENT.toDrawable())
            }
        }

        pullToRefreshFragmentClass.hook(setupMethod, HookStage.AFTER) { param ->
            val root = param.arg<View>(0) as? ViewGroup ?: return@hook
            root.post {
                if (root.getTag(videoInstalledTag) == true) return@post
                root.setTag(videoInstalledTag, true)

                root.addView(
                    videoView(backgroundPath),
                    0,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
        }
    }
}