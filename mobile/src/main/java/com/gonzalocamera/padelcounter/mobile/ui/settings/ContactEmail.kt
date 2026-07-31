package com.gonzalocamera.padelcounter.mobile.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast

private const val CONTACT_EMAIL = "gonzalocamera@gmail.com"

/**
 * Abre el cliente de correo con un mail dirigido al autor de la app.
 *
 * Usa `ACTION_SENDTO` + `mailto:` en vez de `ACTION_SEND`: así solo responden apps
 * de correo y no aparecen WhatsApp, Drive y compañía en la lista.
 */
internal fun contactByEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$CONTACT_EMAIL")
        putExtra(Intent.EXTRA_SUBJECT, "Simple Padel Score — Consulta")
        putExtra(Intent.EXTRA_TEXT, "\n\n${deviceFooter(context)}")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Enviar mail"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No encontramos una app de correo. Escribinos a $CONTACT_EMAIL",
            Toast.LENGTH_LONG,
        ).show()
    }
}

/**
 * Pie con los datos que sirven para diagnosticar un reporte. Se lee la versión del
 * PackageManager y no de BuildConfig, que en este módulo no está generado.
 */
private fun deviceFooter(context: Context): String {
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    return buildString {
        append("---\n")
        append("Simple Padel Score $version\n")
        append("Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}")
    }
}
