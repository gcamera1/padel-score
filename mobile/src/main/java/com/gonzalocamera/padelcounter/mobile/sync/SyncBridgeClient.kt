package com.gonzalocamera.padelcounter.mobile.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Estado del vínculo con el reloj, visto desde el teléfono.
 *
 * Es el espejo de `CompanionStatus` en el módulo :wear.
 */
enum class WatchStatus {
    /** Hay un reloj vinculado con la app del reloj instalada. */
    APP_INSTALLED,
    /** Hay un reloj vinculado pero sin la app. */
    WATCH_NO_APP,
    /** No hay ningún reloj vinculado. */
    NO_WATCH,
    /** No se pudo determinar (error de la API o sin Servicios de Google Play). */
    UNKNOWN
}

/**
 * Detecta el reloj vinculado y abre la ficha de Google Play en él.
 *
 * Espejo de `CompanionDetector` (:wear): la app es una sola ficha con dos form factors, así
 * que cada lado puede ofrecer instalar el otro.
 */
class SyncBridgeClient(private val context: Context) {

    /** Debe coincidir con android_wear_capabilities declarado en el módulo :wear. */
    private val wearCapability = "verify_remote_padel_wear_app"

    suspend fun watchStatus(): WatchStatus {
        return try {
            val capabilityInfo = Wearable.getCapabilityClient(context)
                .getCapability(wearCapability, CapabilityClient.FILTER_ALL)
                .await()
            if (capabilityInfo.nodes.isNotEmpty()) {
                WatchStatus.APP_INSTALLED
            } else {
                val connected = Wearable.getNodeClient(context).connectedNodes.await()
                if (connected.isNotEmpty()) WatchStatus.WATCH_NO_APP else WatchStatus.NO_WATCH
            }
        } catch (_: Exception) {
            WatchStatus.UNKNOWN
        }
    }

    /**
     * Abre la ficha de la app en Google Play **en el reloj** vinculado. Devuelve true si la
     * acción remota se lanzó bien (no si el usuario llegó a instalar).
     *
     * `targetNodeId = null` la manda a todos los nodos cercanos: desde el teléfono, los relojes.
     */
    suspend fun openInstallOnWatch(): Boolean = suspendCancellableCoroutine { cont ->
        val directExecutor = Executor { it.run() }
        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("market://details?id=${context.packageName}"))
            val future = RemoteActivityHelper(context).startRemoteActivity(intent, null)
            future.addListener({
                val ok = try {
                    future.get()
                    true
                } catch (_: Exception) {
                    false
                }
                if (cont.isActive) cont.resume(ok)
            }, directExecutor)
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(false)
        }
    }
}
