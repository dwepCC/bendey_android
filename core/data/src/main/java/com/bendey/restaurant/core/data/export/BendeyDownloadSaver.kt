package com.bendey.restaurant.core.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda un archivo en la carpeta pública "Descargas" del dispositivo (equivalente Android del
 * diálogo "Guardar como" de escritorio: en Android la convención es descargar a Descargas, igual
 * que el navegador o el DownloadManager). Usa MediaStore (scoped storage, minSdk 29) por lo que no
 * requiere permiso WRITE_EXTERNAL_STORAGE.
 *
 * Se usa para todas las descargas de Resto (PDF/ticket de comprobante, listados en PDF/Excel/CSV).
 * El PDF/ticket del comprobante SIEMPRE se genera localmente ([ReceiptPdfService]); nunca se pide
 * al facturador.
 */
@Singleton
class BendeyDownloadSaver @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    /**
     * Copia [source] a Descargas con el nombre [displayName]. Si ya existe un archivo con ese
     * nombre, MediaStore agrega un sufijo " (1)" automáticamente. Devuelve dónde quedó guardado.
     */
    fun saveToDownloads(
        source: File,
        displayName: String,
        mimeType: String,
    ): ExportShareResult {
        if (!source.exists()) {
            return ExportShareResult.Failure("No fue posible generar el archivo.")
        }
        val safeName = sanitizeName(displayName)
        return try {
            val resolver = appContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values)
                ?: return ExportShareResult.Failure("No fue posible crear el archivo en Descargas.")
            resolver.openOutputStream(uri).use { out ->
                if (out == null) {
                    resolver.delete(uri, null, null)
                    return ExportShareResult.Failure("No fue posible escribir el archivo.")
                }
                source.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            ExportShareResult.Success
        } catch (e: Exception) {
            ExportShareResult.Failure("No fue posible guardar el archivo en Descargas.", e)
        }
    }

    private fun sanitizeName(name: String): String {
        val cleaned = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "-")
        return cleaned.ifBlank { "documento" }
    }
}
