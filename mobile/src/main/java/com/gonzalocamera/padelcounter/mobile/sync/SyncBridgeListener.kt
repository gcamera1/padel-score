package com.gonzalocamera.padelcounter.mobile.sync

import android.util.Log
import com.gonzalocamera.padelcounter.mobile.data.MobilePreferences
import com.gonzalocamera.padelcounter.mobile.data.MobileRepository
import com.gonzalocamera.padelcounter.mobile.data.db.PadelDatabase
import com.gonzalocamera.padelcounter.shared.MatchDecodeException
import com.gonzalocamera.padelcounter.shared.decodeMatch
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SyncBridgeListener : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val db = PadelDatabase.getInstance(applicationContext)
        val preferences = MobilePreferences(applicationContext)
        val repository = MobileRepository(db.matchDao(), preferences)

        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith("/padel-score/match/")) continue

            try {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val bytes = dataMap.getByteArray("match_data") ?: continue
                val match = decodeMatch(bytes)

                scope.launch {
                    repository.insertMatch(match)
                }
            } catch (e: MatchDecodeException) {
                Log.e("SyncBridge", "Failed to decode match from wear: ${e.message}")
            }
        }
    }
}
