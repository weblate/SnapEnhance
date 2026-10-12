package me.rhunk.snapenhance.core.features.impl.downloader

import android.view.Gravity
import android.widget.FrameLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.event.events.impl.NetworkApiRequestEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.ViewAppearanceHelper

class ProfilePictureDownloader : Feature("ProfilePictureDownloader") {
    private val publicProfileViewIconSize by lazy { context.userInterface.dpToPx(30) }
    private val publicProfileViewIconMargin by lazy { context.userInterface.dpToPx(15) }
    private val publicProfileViewIconMarginTop by lazy { context.userInterface.dpToPx(80) }

    override fun init() {
        if (!context.config.downloader.downloadProfilePictures.get()) return

        var friendUsername: String? = null
        var backgroundUrl: String? = null
        var avatarUrl: String? = null

        onNextActivityCreate(defer = true) {
            context.event.subscribe(AddViewEvent::class) { event ->
                if (event.view::class.java.name != "com.snap.unifiedpublicprofile.UnifiedPublicProfileView") return@subscribe

                event.view.post {
                    val parent = event.view.parent as? FrameLayout ?: return@post

                    parent.addView(
                        createComposeView(parent.context) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }.apply {
                            setOnClickListener {
                                ViewAppearanceHelper.newAlertDialogBuilder(
                                    this@ProfilePictureDownloader.context.mainActivity!!
                                ).apply {
                                    setTitle(this@ProfilePictureDownloader.context.translation["profile_picture_downloader.title"])
                                    val choices = mutableMapOf<String, String>()
                                    backgroundUrl?.let { choices["background_option"] = it }
                                    avatarUrl?.let { choices["avatar_option"] = it }

                                    setItems(choices.keys.map {
                                        this@ProfilePictureDownloader.context.translation["profile_picture_downloader.$it"]
                                    }.toTypedArray()) { _, which ->
                                        runCatching {
                                            this@ProfilePictureDownloader.context.feature(MediaDownloader::class).downloadProfilePicture(
                                                choices.values.elementAt(which),
                                                friendUsername!!
                                            )
                                        }.onFailure {
                                            this@ProfilePictureDownloader.context.log.error("Failed to download profile picture", it)
                                        }
                                    }
                                }.show()
                            }
                        },

                        FrameLayout.LayoutParams(
                            publicProfileViewIconSize,
                            publicProfileViewIconSize,
                            Gravity.TOP or Gravity.START
                        ).apply {
                            topMargin = publicProfileViewIconMarginTop
                            marginStart = publicProfileViewIconMargin
                        }
                    )
                }
            }

            context.event.subscribe(NetworkApiRequestEvent::class) { event ->
                if (!event.url.endsWith("/rpc/getPublicProfile")) return@subscribe

                event.onSuccess { buffer ->
                    ProtoReader(buffer ?: return@onSuccess).followPath(1, 1, 2) {
                        friendUsername = getString(2) ?: return@followPath

                        followPath(4) {
                            backgroundUrl = getString(2)
                            avatarUrl = getString(100)
                        }
                    }
                }
            }
        }
    }
}