package com.gonzalocamera.padelcounter.presentation

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
 * Estado del vínculo con la app de teléfono (companion).
 *
 * La app de reloj es companion (standalone = false): el sync de partidos depende
 * de la app de teléfono. Este detector permite avisar al usuario y guiarlo a
 * instalarla cuando falta, sin bloquear el uso del marcador.
 */
enum class CompanionStatus {
    /** Hay un teléfono con la app de teléfono instalada. */
    APP_INSTALLED,
    /** Hay un teléfono vinculado pero sin la app instalada. */
    PHONE_NO_APP,
    /** No hay ningún teléfono vinculado. */
    NO_PHONE,
    /** No se pudo determinar (error de la API). */
    UNKNOWN
}

object CompanionDetector {

    /** Debe coincidir con android_wear_capabilities declarado en el módulo :mobile. */
    private const val PHONE_CAPABILITY = "verify_remote_padel_phone_app"
    private const val APP_ID = "com.gonzalocamera.padelcounter"

    suspend fun detect(context: Context): CompanionStatus {
        return try {
            val capabilityInfo = Wearable.getCapabilityClient(context)
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_ALL)
                .await()
            if (capabilityInfo.nodes.isNotEmpty()) {
                CompanionStatus.APP_INSTALLED
            } else {
                val connected = Wearable.getNodeClient(context).connectedNodes.await()
                if (connected.isNotEmpty()) CompanionStatus.PHONE_NO_APP else CompanionStatus.NO_PHONE
            }
        } catch (_: Exception) {
            CompanionStatus.UNKNOWN
        }
    }

    /**
     * Abre la ficha de la app en Google Play en el teléfono vinculado usando
     * RemoteActivityHelper. Devuelve true si la acción remota se lanzó bien.
     */
    suspend fun openInstallOnPhone(context: Context): Boolean = suspendCancellableCoroutine { cont ->
        val directExecutor = Executor { it.run() }
        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("market://details?id=$APP_ID"))
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
