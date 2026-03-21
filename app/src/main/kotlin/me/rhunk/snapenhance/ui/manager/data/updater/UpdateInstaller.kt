package me.rhunk.snapenhance.ui.manager.data.updater

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import me.rhunk.snapenhance.common.logger.AbstractLogger
import java.io.File

fun installApk(context: Context, apkFile: File, logger: AbstractLogger) {
    try {
        // github.com/techyourchance/TechYourChance-Android-Application/blob/c9af344c78e17d860cea2dcb272f7c7543a8d324/app/src/main/java/com/techyourchance/android/apkupdate/UpdateApkUseCase.kt#L59-L73
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        logger.error("Failed to launch system installer", e)
    }
}