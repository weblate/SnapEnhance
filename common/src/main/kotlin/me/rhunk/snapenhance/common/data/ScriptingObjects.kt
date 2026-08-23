package me.rhunk.snapenhance.common.data

data class RepositoryScriptManifest(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val author: String? = null,
    val version: String? = null,
    val filepath: String? = null
)