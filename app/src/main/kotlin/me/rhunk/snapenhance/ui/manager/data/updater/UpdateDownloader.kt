package me.rhunk.snapenhance.ui.manager.data.updater

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream
import me.rhunk.snapenhance.ui.manager.data.updater.UpdateFetcher.latestRelease

private val scope = CoroutineScope(Dispatchers.IO)
private val client = OkHttpClient()

val isDownloading = MutableStateFlow(false)
val isReadyToInstall = MutableStateFlow(false)

private fun getBaseDownloadDir(context: Context): File {
    return context.getExternalFilesDir(null) ?: throw IllegalStateException("External storage unavailable")
}

private fun unzip(zip: File, dir: File, logger: AbstractLogger): File? {
    val destFolder = File(dir, "update_extract_${System.currentTimeMillis()}")
    if (!destFolder.exists()) destFolder.mkdirs()
    var apkFile: File? = null
    try {
        zip.inputStream().use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destFolder, entry.name)
                    if (!entry.isDirectory) {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zis.copyTo(it) }
                        if (file.extension.lowercase() == "apk") {
                            apkFile = file
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    } catch (e: Exception) {
        logger.error("Unzip failed", e)
    }
    return apkFile
}

fun downloadUpdate(context: Context, logger: AbstractLogger) {
    val release = latestRelease.value ?: return
    val url = release.releaseUrl

    scope.launch {
        isDownloading.value = true
        val downloadBaseDir = getBaseDownloadDir(context)
        val downloadedZip = File(downloadBaseDir, "update_temp.zip")

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

                val body = response.body

                downloadedZip.outputStream().use { output ->
                    val input = body.byteStream()
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalDownloaded += read
                    }
                    logger.debug("Download finished, Received $totalDownloaded bytes.")
                }

                val apkFile = unzip(downloadedZip, downloadBaseDir, logger)
                if (apkFile != null && apkFile.exists()) {
                    isReadyToInstall.value = true
                    isDownloading.value = false
                    installApk(context, apkFile, logger)
                } else {
                    isReadyToInstall.value = false
                    throw Exception("no apk found in the downloaded artifacts")
                }

            }
        } catch (e: Exception) {
            logger.error("Update failed", e)
            isDownloading.value = false
            isReadyToInstall.value = false
        } finally {
            if (downloadedZip.exists()) downloadedZip.delete()
        }
    }
}