package com.gonzalocamera.padelcounter.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.decodeMatch
import com.gonzalocamera.padelcounter.shared.encodeMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.syncDataStore by preferencesDataStore(name = "wear_sync_queue")

class WearSyncQueue(private val context: Context) {

    private val KEY_QUEUE = stringPreferencesKey("pending_matches")

    suspend fun enqueue(match: Match) {
        context.syncDataStore.edit { prefs ->
            val current = deserializeQueue(prefs[KEY_QUEUE])
            val updated = current + match
            prefs[KEY_QUEUE] = serializeQueue(updated)
        }
    }

    suspend fun dequeueAll(): List<Match> {
        val matches = context.syncDataStore.data.first().let { prefs ->
            deserializeQueue(prefs[KEY_QUEUE])
        }
        context.syncDataStore.edit { prefs -> prefs.remove(KEY_QUEUE) }
        return matches
    }

    suspend fun isEmpty(): Boolean {
        return context.syncDataStore.data.first().let { prefs ->
            deserializeQueue(prefs[KEY_QUEUE]).isEmpty()
        }
    }

    val pendingCount: Flow<Int> = context.syncDataStore.data.map { prefs ->
        deserializeQueue(prefs[KEY_QUEUE]).size
    }

    private fun serializeQueue(matches: List<Match>): String {
        val encoded = matches.map { String(encodeMatch(it), Charsets.UTF_8) }
        return Json.encodeToString(encoded)
    }

    private fun deserializeQueue(json: String?): List<Match> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val strings: List<String> = Json.decodeFromString(json)
            strings.map { decodeMatch(it.toByteArray(Charsets.UTF_8)) }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
