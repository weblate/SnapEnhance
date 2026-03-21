package me.rhunk.snapenhance.ui.manager.data.updater

import com.google.gson.JsonParser
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.UpdateNotification

object UpdateFetcher {
    data class LatestRelease(
        val versionName: String,
        val releaseUrl: String
    )

    private val _latestRelease = MutableStateFlow<LatestRelease?>(null)
    val latestRelease: StateFlow<LatestRelease?> = _latestRelease.asStateFlow()

    private var lastCheckTime: Long = 0
    private const val CACHE_DURATION_MS = 60 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getLatestUpdate(): LatestRelease? {
        val currentTime = System.currentTimeMillis()
        if (_latestRelease.value != null && (currentTime - lastCheckTime) < CACHE_DURATION_MS) {
            return _latestRelease.value
        }

        return runBlocking(Dispatchers.IO) {
            val result = if (BuildConfig.DEBUG) fetchLatestDebugCI() else fetchLatestRelease()
            lastCheckTime = currentTime
            _latestRelease.value = result
            result
        }
    }

    private fun fetchLatestDebugCI(): LatestRelease? = runCatching {
        val url = "https://git.seextended.org/api/v1/repos/bocajthomas/SE-Extended/actions/runs?event=workflow_dispatch"
        val response = client.newCall(Request.Builder().url(url).build()).execute()

        val bodyString = response.body?.string() ?: throw Throwable("Empty response body")

        if (!response.isSuccessful) {
            AbstractLogger.directError("Failed to fetch CI Build:", Throwable("${response.code}"))
        }

        val json = try {
            JsonParser.parseString(bodyString).asJsonObject
        } catch (e: Exception) {
            AbstractLogger.directError("invalid JSON: $bodyString", e)
            throw e
        }

        val runs = json.getAsJsonArray("workflow_runs") ?: throw Throwable("No runs array")

        val latestRun = runs.map { it.asJsonObject }.filter { run ->
            val isSuccess = run.get("status")?.asString == "success"
            val workflowFile = run.get("workflow_id")?.asString ?: ""
            val branch = run.get("prettyref")?.asString ?: ""

            isSuccess && workflowFile == "debug.yml" && branch == "dev"
        }.maxByOrNull { it.get("id").asInt } ?: return@runCatching null

        val commitSha = latestRun.get("commit_sha").asString
        val runId = latestRun.get("id").asInt

        if (commitSha == BuildConfig.GIT_HASH) {
            return@runCatching null
        }

        val arch = System.getProperty("os.arch") ?: "arm64-v8a"
        val artifactName = if (arch.contains("64")) "SE-Extended-ARMV8-Debug" else "SE-Extended-ARMV7-Debug"

        LatestRelease(
            versionName = "${commitSha.take(7)}-debug",
            releaseUrl = "https://git.seextended.org/bocajthomas/SE-Extended/actions/runs/$runId/artifacts/$artifactName"
        )
    }.onFailure {
        AbstractLogger.directError("CI Fetch Failed", it)
    }.getOrNull()

    // TODO: when closer to a stable v1.0.0 release, fix to actually get the latest stable releases
    private fun fetchLatestRelease(): LatestRelease? = runCatching {
        val url = "https://git.seextended.org/bocajthomas/SE-Extended/releases"
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val releases = JsonParser.parseString(response.body?.string()).asJsonArray
        if (releases.size() == 0) return@runCatching null

        val latest = releases[0].asJsonObject
        val tagName = latest.get("tag_name").asString
        if (tagName.replace("v", "") == BuildConfig.VERSION_NAME) return@runCatching null

        LatestRelease(versionName = tagName, releaseUrl = latest.get("html_url").asString)
    }.getOrNull()

    fun checkForUpdates(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val update = getLatestUpdate()
            if (update != null) {
                withContext(Dispatchers.Main) {
                    UpdateNotification.showUpdateNotification(context, update)
                }
            }
        }
    }

    fun fetchUpdateInBackground(onResult: (LatestRelease?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val update = getLatestUpdate()
                withContext(Dispatchers.Main) {
                    onResult(update)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            }
        }
    }
}