package me.rhunk.snapenhance.common.data

data class RepositoryIndex(
    val themes: List<RepositoryThemeManifest> = emptyList(),
    val scripts: List<RepositoryScriptManifest> = emptyList()
)