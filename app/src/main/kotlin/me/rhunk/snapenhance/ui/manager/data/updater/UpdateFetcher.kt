package me.rhunk.snapenhance.ui.manager.data.updater

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.UpdateNotification
import me.rhunk.snapenhance.common.BuildConfig
import me.rhunk.snapenhance.common.logger.AbstractLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object UpdateFetcher {
    data class LatestRelease(
        val versionName: String,
        val releaseUrl: String
    )

    var cachedRelease: LatestRelease? = null
        private set

    val latestRelease: LatestRelease? get() = cachedRelease

    private var lastCheckTime: Long = 0
    private const val CACHE_DURATION_MS = 60 * 60 * 1000L

    private const val FORGEJO_BASE_URL = "https://git.seextended.org/api/v1"
    private const val REPO_PATH = "bocajthomas/SE-Extended"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getLatestUpdate(): LatestRelease? {
        val currentTime = System.currentTimeMillis()
        if (cachedRelease != null && (currentTime - lastCheckTime) < CACHE_DURATION_MS) {
            return cachedRelease
        }

        val result = if (BuildConfig.DEBUG) fetchLatestDebugCI() else fetchLatestRelease()
        lastCheckTime = currentTime
        cachedRelease = result
        return result
    }

    private fun fetchLatestRelease() = runCatching {
        val endpoint = "$FORGEJO_BASE_URL/repos/$REPO_PATH/releases"
        val request = Request.Builder().url(endpoint).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Throwable("Forgejo API Error: ${response.code}")

        val bodyString = response.body?.string() ?: throw Throwable("Empty response")
        val releases = JsonParser.parseString(bodyString).asJsonArray

        if (releases.size() == 0) {
            AbstractLogger.directDebug("Update Check: No releases found on Forgejo.")
            return@runCatching null
        }

        val latestRelObj = releases.get(0).asJsonObject
        val latestVersion = latestRelObj.getAsJsonPrimitive("tag_name").asString

        if (latestVersion.removePrefix("v") == BuildConfig.VERSION_NAME) {
            AbstractLogger.directDebug("No updates found. Using latest version: $latestVersion")
            return@runCatching null
        }

        LatestRelease(
            versionName = latestVersion,
            releaseUrl = "https://git.seextended.org/$REPO_PATH/releases/latest"
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest Forgejo release", it)
    }.getOrNull()

    private fun fetchLatestDebugCI() = runCatching {
        val endpoint = "$FORGEJO_BASE_URL/repos/$REPO_PATH/actions/runs"
        val request = Request.Builder().url(endpoint).build()

        val response = client.newCall(request).execute()

        if (response.code == 202) {
            AbstractLogger.directDebug("Forgejo Actions API is rebuilding cache (202).")
            return@runCatching null
        }

        if (!response.isSuccessful) throw Throwable("Forgejo Actions Error: ${response.code}")

        val bodyString = response.body?.string() ?: throw Throwable("Empty CI body")
        val actionRuns = JsonParser.parseString(bodyString).asJsonObject
        val workflowRuns = actionRuns.getAsJsonArray("workflow_runs") ?: return@runCatching null

        val latestRun = workflowRuns.map { it.asJsonObject }.firstOrNull { run ->
            val status = run.getAsJsonPrimitive("status")?.asString
            val conclusion = run.getAsJsonPrimitive("conclusion")?.asString
            status == "completed" && conclusion == "success"
        }

        if (latestRun == null) {
            AbstractLogger.directDebug("Update Check: No successful Action runs found.")
            return@runCatching null
        }

        val headSha = latestRun.getAsJsonPrimitive("head_sha")?.asString ?: return@runCatching null

        if (headSha == BuildConfig.GIT_HASH) {
            AbstractLogger.directDebug("No CI updates found. Hash $headSha is current.")
            return@runCatching null
        }
        val arch = System.getProperty("os.arch") ?: "arm64-v8a"
        val apkName = if (arch.contains("64", ignoreCase = true)) {
            "SE-Extended-ARMV8-Debug.zip"
        } else {
            "SE-Extended-ARMV7-Debug.zip"
        }

        LatestRelease(
            versionName = headSha.take(7) + "-debug",
            releaseUrl = latestRun.getAsJsonPrimitive("html_url").asString + "/$apkName"
        )
    }.onFailure {
        AbstractLogger.directError("Failed to fetch latest Forgejo debug CI", it)
    }.getOrNull()

    fun checkForUpdates(context: Context, isManual: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val update = getLatestUpdate()
                withContext(Dispatchers.Main) {
                    if (update != null) {
                        UpdateNotification.showNotification(context, update)
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    AbstractLogger.directError("Check Failed", e)
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
                    onResult(cachedRelease)
                }
            }
        }
    }
}