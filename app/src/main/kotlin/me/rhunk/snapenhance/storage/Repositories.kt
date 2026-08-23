package me.rhunk.snapenhance.storage

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.common.data.RepositoryIndex
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull
import me.rhunk.snapenhance.ui.manager.data.BUILT_IN_REPOSITORIES
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

fun AppDatabase.getRepositories(): List<String> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT url FROM repositories", null).use { cursor ->
            val repos = mutableListOf<String>()
            while (cursor.moveToNext()) {
                repos.add(cursor.getStringOrNull("url") ?: continue)
            }
            repos
        }
    }
}

fun AppDatabase.removeRepo(url: String) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.delete("repositories", "url = ?", arrayOf(url))
    }
}

fun AppDatabase.addRepo(url: String) {
    val values = ContentValues().apply { put("url", url) }
    database.insertWithOnConflict("repositories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
}

fun AppDatabase.initBuiltInRepos() {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val gson = context.gson
    CoroutineScope(Dispatchers.IO).launch {
        BUILT_IN_REPOSITORIES.map { url ->
            async {
                var modifiedUrl = url

                val success = runCatching {
                    if (url.contains("git.seextended.org")) {
                        val pathSegments = url.removePrefix("https://git.seextended.org/").split("/")
                        if (pathSegments.size >= 2) {
                            val owner = pathSegments[0]
                            val repo = pathSegments[1]

                            val apiReq = Request.Builder()
                                .url("https://git.seextended.org/api/v1/repos/$owner/$repo")
                                .build()

                            okHttpClient.newCall(apiReq).execute().use { response ->
                                if (!response.isSuccessful) {
                                    context.log.warn("Forgejo API failed for $repo: ${response.code}")
                                    return@runCatching false
                                }
                                val json = response.body?.string() ?: return@runCatching false
                                val defaultBranch = gson.fromJson(json, Map::class.java)["default_branch"] as String

                                modifiedUrl = "https://git.seextended.org/$owner/$repo/raw/branch/$defaultBranch/"
                                context.log.info("Resolved Forgejo branch for $repo: $defaultBranch")
                            }
                        }
                    } else if (url.startsWith("https://github.com/")) {
                        val splitUrl = modifiedUrl.removePrefix("https://github.com/").split("/")
                        if (splitUrl.size >= 2) {
                            val repoName = "${splitUrl[0]}/${splitUrl[1]}"

                            val request = Request.Builder().url("https://api.github.com/repos/$repoName").build()
                            okHttpClient.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) return@runCatching false
                                val json = response.body?.string() ?: return@runCatching false
                                val defaultBranch = gson.fromJson(json, Map::class.java)["default_branch"] as String
                                modifiedUrl = "https://raw.githubusercontent.com/$repoName/$defaultBranch/"
                            }
                        }
                    }
                    true
                }.onFailure {
                    context.log.error("Failed to resolve repository metadata for $url", it)
                }.getOrDefault(false)

                if (!success) return@async

                val indexUri = modifiedUrl.toUri().buildUpon().appendPath("index.json").build()

                runCatching {
                    val request = Request.Builder().url(indexUri.toString()).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            context.log.warn("Failed to fetch index for built-in repo $indexUri: ${response.code}")
                            return@runCatching
                        }
                        gson.fromJson(response.body?.charStream(), RepositoryIndex::class.java)?.also {
                            context.log.info("Validated built-in repository index: $it")
                        }

                        addRepo(modifiedUrl)
                    }
                }.onFailure {
                    context.log.error("Failed to validate or parse built-in repository index: $indexUri", it)
                }
            }
        }.awaitAll()
    }
}