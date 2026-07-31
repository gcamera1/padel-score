package com.gonzalocamera.padelcounter.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Versión del formato de backup. Subir solo ante un cambio incompatible. */
const val ARCHIVE_VERSION = 1

/**
 * Backup completo del historial. El wrapper existe para poder evolucionar el formato:
 * sin [version] un archivo viejo sería indistinguible de uno nuevo.
 */
@Serializable
data class MatchArchive(
    val version: Int = ARCHIVE_VERSION,
    val exportedAt: Long,
    val matches: List<Match>,
)

class ArchiveDecodeException(message: String) : Exception(message)

// prettyPrint: el backup queda legible si el usuario lo abre; ignoreUnknownKeys deja
// que una versión vieja de la app lea un archivo con campos que todavía no conoce.
// encodeDefaults es imprescindible: sin él kotlinx omite todo campo igual a su default,
// y `version` (= ARCHIVE_VERSION) no se escribiría. El día que ARCHIVE_VERSION pase a 2,
// los backups viejos se leerían como v2 y el chequeo de compatibilidad sería inútil.
private val archiveJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

fun encodeArchive(archive: MatchArchive): String = archiveJson.encodeToString(archive)

/**
 * Parsea un backup. Lanza [ArchiveDecodeException] con un motivo legible ante
 * cualquier archivo que no sea un historial válido de esta app.
 */
fun decodeArchive(text: String): MatchArchive {
    if (text.isBlank()) throw ArchiveDecodeException("El archivo está vacío")

    val archive = try {
        archiveJson.decodeFromString<MatchArchive>(text)
    } catch (e: Exception) {
        throw ArchiveDecodeException("El archivo no es un historial de Simple Padel Score")
    }

    if (archive.version > ARCHIVE_VERSION) {
        throw ArchiveDecodeException(
            "El archivo lo generó una versión más nueva de la app. Actualizala para importarlo."
        )
    }
    return archive
}
