package me.rhunk.snapenhance.core.action.impl

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OpenParams
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import me.rhunk.snapenhance.common.data.FileType
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedMiddle
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.createComposeAlertDialog
import me.rhunk.snapenhance.common.util.ktx.getLongOrNull
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import me.rhunk.snapenhance.core.action.AbstractAction
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.absoluteValue

class ExportMemories : AbstractAction() {
    data class TimeRange(
        val start: Long?,
        val end: Long?,
    )

    data class MemoriesEntry(
        val storyTitle: String,
        val createTime: Long,
        val mediaKey: String?,
        val mediaIv: String?,
        val downloadUrl: String
    ) {
        val folderName: String
            get() = storyTitle.replace(Regex("[^a-zA-Z0-9\\s]"), "").trim().replace(Regex("\\s+"), "_")
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalEncodingApi::class)
    private suspend fun exportMemories(
        scope: CoroutineScope = context.coroutineScope,
        database: SQLiteDatabase,
        timeRange: TimeRange?,
        includeMEO: Boolean,
        folders: Boolean,
        progress: (Int, Int) -> Unit
    ) {
        val downloadContext = Dispatchers.IO.limitedParallelism(10)
        val writeToZipContext = Dispatchers.IO.limitedParallelism(1)
        val outputZip = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "memories_" + System.currentTimeMillis() + ".zip").also {
            if (it.exists()) it.delete()
        }
        val okHttpClient = OkHttpClient.Builder().build()
        val outputZipFile = withContext(Dispatchers.IO) {
            ZipOutputStream(FileOutputStream(outputZip)).apply {
                setComment("Exported from SnapEnhance")
                setMethod(ZipOutputStream.DEFLATED)
            }
        }
        var totalCount = 0
        var currentCount = 0
        var failed = 0

        fun updateProgress() {
            progress((currentCount.toFloat() / totalCount.toFloat() * 100f).toInt(), failed)
        }

        val jobs = mutableListOf<Job>()

        val meoMasterKeyPair = if (includeMEO) {
            runCatching {
                database.rawQuery("SELECT * FROM memories_meo_confidential", null).use { cursor ->
                    if (cursor.moveToNext()) {
                        cursor.getStringOrNull("master_key")!!.trim() to cursor.getStringOrNull("master_key_iv")!!.trim()
                    } else null
                }
            }.getOrNull()
        } else null

        database.rawQuery("SELECT memories_entry.title as story_title, memories_snap.create_time, " +
                "memories_snap.media_key, memories_snap.media_iv, memories_snap.encrypted_media_key, memories_snap.encrypted_media_iv, " +
                "memories_media.download_url FROM memories_snap " +
            "INNER JOIN memories_entry ON memories_snap.memories_entry_id = memories_entry._id " +
            "INNER JOIN memories_media ON memories_snap.media_id = memories_media._id " +
            "WHERE memories_snap.create_time >= ? AND memories_snap.create_time <= ? " +
            "ORDER BY memories_snap.create_time ASC", arrayOf(timeRange?.start?.toString() ?: "-1", timeRange?.end?.toString() ?: Long.MAX_VALUE.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val encryptedMediaKey = cursor.getStringOrNull("encrypted_media_key")?.trim()
                val encryptedMediaIv = cursor.getStringOrNull("encrypted_media_iv")?.trim()
                var mediaKey = cursor.getStringOrNull("media_key")?.trim()
                var mediaIv = cursor.getStringOrNull("media_iv")?.trim()

                if (!includeMEO && encryptedMediaKey != null && encryptedMediaIv != null) continue

                meoMasterKeyPair.takeIf { encryptedMediaKey != null && encryptedMediaIv != null }?.let { keyPair ->
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    runCatching {
                        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(Base64.decode(keyPair.first), "AES"), IvParameterSpec(Base64.decode(keyPair.second)))
                        mediaKey = Base64.encode(cipher.doFinal(Base64.decode(encryptedMediaKey ?: return@let)))
                        mediaIv = Base64.encode(cipher.doFinal(Base64.decode(encryptedMediaIv ?: return@let)))
                        context.log.verbose("decrypted meo $mediaKey/$mediaIv")
                    }.onFailure {
                        context.log.error("failed to decrypt meo", it)
                    }
                }

                if (mediaKey == null || mediaIv == null) {
                    context.log.error("missing media key or iv for ${cursor.getStringOrNull("download_url")}")
                    failed++
                    updateProgress()
                    continue
                }

                val entry = MemoriesEntry(
                    storyTitle = cursor.getStringOrNull("story_title") ?: "unknown",
                    createTime = cursor.getLongOrNull("create_time") ?: -1L,
                    mediaKey = mediaKey,
                    mediaIv = mediaIv,
                    downloadUrl = cursor.getStringOrNull("download_url") ?: continue
                )

                totalCount++

                scope.launch(downloadContext) {
                    var downloadedFile = File.createTempFile("memories", ".tmp", context.androidContext.cacheDir)

                    runCatching {
                        okHttpClient.newCall(
                            okhttp3.Request.Builder()
                                .url(entry.downloadUrl)
                                .build()
                        ).execute().use { response ->
                            val inputStream  = response.body.byteStream().let {
                                if (entry.mediaKey != null && entry.mediaIv != null) {
                                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(Base64.decode(entry.mediaKey), "AES"), IvParameterSpec(Base64.decode(entry.mediaIv)))
                                    CipherInputStream(it, cipher)
                                } else it
                            }

                            downloadedFile.outputStream().use { outputStream ->
                                inputStream.use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            val fileType = FileType.fromFile(downloadedFile)

                            downloadedFile = File(
                                downloadedFile.parentFile,
                                "${entry.createTime}-${entry.downloadUrl.hashCode().absoluteValue.toString(16)}.${fileType.fileExtension}"
                            ).also {
                                downloadedFile.renameTo(it)
                            }

                            withContext(writeToZipContext) {
                                val zipEntry = ZipEntry("${if (folders) entry.folderName + "/" else entry.folderName}${downloadedFile.name}")
                                FileTime.fromMillis(entry.createTime).let {
                                    zipEntry.lastModifiedTime = it
                                    zipEntry.lastAccessTime = it
                                    zipEntry.creationTime = it
                                }
                                outputZipFile.apply {
                                    putNextEntry(zipEntry)
                                    downloadedFile.inputStream().use { it.copyTo(outputZipFile) }
                                    closeEntry()
                                    flush()
                                }
                                currentCount++
                                updateProgress()
                            }
                        }
                    }.onFailure {
                        context.log.error("failed to download ${entry.downloadUrl}", it)
                        failed++
                        updateProgress()
                    }
                    downloadedFile.delete()
                }.also { jobs.add(it) }
            }
        }

        jobs.joinAll()
        withContext(Dispatchers.IO) {
            outputZipFile.close()
        }
        context.longToast("Exported to ${outputZip.absolutePath}")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ExporterDialog(database: SQLiteDatabase, onDismiss: () -> Unit) {
        var exportJob by remember { mutableStateOf(null as Job?) }
        var exportFinished by remember { mutableStateOf(false) }
        var exportProgress by remember { mutableStateOf(Pair(0, 0)) } // progress, failed

        var dateRangeFilter by remember { mutableStateOf(false) }
        var sortByFolder by remember { mutableStateOf(false) }
        var includeMEO by remember { mutableStateOf(false) }
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = OffsetDateTime.now().minusDays(8).toInstant().toEpochMilli(),
            initialSelectedEndDateMillis = Instant.now().toEpochMilli(),
            initialDisplayMode = DisplayMode.Input
        )

        var dateRangeDialog by remember { mutableStateOf(false) }

        val totalCount = remember(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis, dateRangeFilter) {
            val timeRange = dateRangePickerState.takeIf { dateRangeFilter }?.let {
                TimeRange(it.selectedStartDateMillis, it.selectedEndDateMillis)
            }

            database.rawQuery("SELECT COUNT(*) FROM memories_snap WHERE create_time >= ? AND create_time <= ? ", arrayOf(timeRange?.start?.toString() ?: "-1", timeRange?.end?.toString() ?: Long.MAX_VALUE.toString())).use {
                it.moveToFirst()
                it.getInt(0)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Export memories",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (exportJob != null) {
                Text(
                    text = "Exporting memories... (${exportProgress.second} failed)",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LinearWavyProgressIndicator(
                    progress = { exportProgress.first / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    Button(
                        onClick = {
                            exportJob?.cancel()
                            exportJob = null
                            onDismiss()
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Quit")
                    }

                    if (exportFinished) {
                        Button(
                            onClick = {
                                exportJob = null
                                onDismiss()
                            },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Done")
                        }
                    }
                }
            } else {
                Text(
                    text = "Total memories: $totalCount",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 55.dp)
                                .clickable { dateRangeFilter = !dateRangeFilter }
                                .padding(start = 15.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = dateRangeFilter,
                                onCheckedChange = { dateRangeFilter = it }
                            )
                            Text(
                                text = "Date Range",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedButton(
                                onClick = { dateRangeDialog = true },
                                enabled = dateRangeFilter,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text("Select")
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 55.dp)
                                .clickable { sortByFolder = !sortByFolder }
                                .padding(start = 15.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sortByFolder,
                                onCheckedChange = { sortByFolder = it }
                            )
                            Text(
                                text = "Sort by folder",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 55.dp)
                                .clickable { includeMEO = !includeMEO }
                                .padding(start = 15.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeMEO,
                                onCheckedChange = { includeMEO = it }
                            )
                            Text(
                                text = "Include My Eyes Only",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    Button(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            context.coroutineScope.launch {
                                exportMemories(
                                    scope = this,
                                    database = database,
                                    timeRange = dateRangePickerState.takeIf { dateRangeFilter }?.let {
                                        TimeRange(it.selectedStartDateMillis, it.selectedEndDateMillis)
                                    },
                                    folders = sortByFolder,
                                    includeMEO = includeMEO,
                                ) { progress, failed ->
                                    exportProgress = Pair(progress, failed)
                                }
                            }.also { exportJob = it }.invokeOnCompletion {
                                exportFinished = true
                            }
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text("Export")
                    }
                }

                if (dateRangeDialog) {
                    DatePickerDialog(
                        onDismissRequest = { dateRangeDialog = false },
                        confirmButton = {
                            Button(
                                onClick = { dateRangeDialog = false },
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { dateRangeDialog = false },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Cancel")
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // https://issuetracker.google.com/issues/325309575#comment1
                        DateRangePicker(
                            modifier = Modifier.weight(1f),
                            state = dateRangePickerState,
                            title = {
                                Text(
                                    text = "Select date range",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp),
                                )
                            },
                            headline = {
                                DateRangePickerDefaults.DateRangePickerHeadline(
                                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                                    displayMode = dateRangePickerState.displayMode,
                                    dateFormatter = remember { DatePickerDefaults.dateFormatter() },
                                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    override fun run() {
        context.coroutineScope.launch(Dispatchers.Main) {
            val database = runCatching {
                SQLiteDatabase.openDatabase(
                    context.androidContext.getDatabasePath("memories.db"),
                    OpenParams.Builder().setOpenFlags(SQLiteDatabase.OPEN_READONLY).build()
                )
            }.getOrNull()

            if (database == null) {
                context.longToast("Failed to open memories database")
                return@launch
            }

            createComposeAlertDialog(context.mainActivity!!) { alertDialog ->
                ExporterDialog(database) { alertDialog.dismiss() }
            }.apply {
                setOnDismissListener { database.close() }
                setCanceledOnTouchOutside(false)
                show()
            }
        }
    }
}