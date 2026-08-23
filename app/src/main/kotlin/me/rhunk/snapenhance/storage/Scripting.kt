package me.rhunk.snapenhance.storage

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

fun AppDatabase.setScriptEnabled(name: String, enabled: Boolean) {
    executeAsync {
        if (enabled) {
            database.execSQL("INSERT OR REPLACE INTO enabled_scripts (name) VALUES (?)", arrayOf(name))
        } else {
            database.execSQL("DELETE FROM enabled_scripts WHERE name = ?", arrayOf(name))
        }
    }
}

fun AppDatabase.isScriptEnabled(name: String): Boolean {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT * FROM enabled_scripts WHERE name = ?", arrayOf(name)).use {
            it.moveToNext()
        }
    }
}
