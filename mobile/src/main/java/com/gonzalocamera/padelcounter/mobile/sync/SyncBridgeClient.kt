package com.gonzalocamera.padelcounter.mobile.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class SyncBridgeClient(private val context: Context) {

    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun isWearAppInstalled(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun hasConnectedWatch(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
