package com.gonzalocamera.padelcounter.sync

import android.content.Context
import com.gonzalocamera.padelcounter.shared.Match
import com.gonzalocamera.padelcounter.shared.encodeMatch
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearSyncSender(
    private val context: Context,
    private val queue: WearSyncQueue
) {
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    suspend fun trySendPending(): SyncResult {
        if (!isMobileReachable()) {
            return if (queue.isEmpty()) SyncResult.NoMobileApp else SyncResult.Unreachable
        }

        val pending = queue.dequeueAll()
        if (pending.isEmpty()) return SyncResult.Success(0)

        var sent = 0
        val failed = mutableListOf<Match>()

        for (match in pending) {
            try {
                val request = PutDataMapRequest.create("/padel-score/match/${match.id}").apply {
                    dataMap.putByteArray("match_data", encodeMatch(match))
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                dataClient.putDataItem(request).await()
                sent++
            } catch (_: Exception) {
                failed.add(match)
            }
        }

        if (failed.isNotEmpty()) {
            for (m in failed) queue.enqueue(m)
            return SyncResult.PartialFailure(sent, failed.size)
        }

        return SyncResult.Success(sent)
    }

    suspend fun isMobileReachable(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.any { !it.isNearby || it.isNearby }
        } catch (_: Exception) {
            false
        }
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class PartialFailure(val sent: Int, val failed: Int) : SyncResult()
        data object NoMobileApp : SyncResult()
        data object Unreachable : SyncResult()
    }
}
