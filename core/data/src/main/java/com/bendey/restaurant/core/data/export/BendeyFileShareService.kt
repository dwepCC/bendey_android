package com.bendey.restaurant.core.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BendeyFileShareService @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val authority: String
        get() = "${appContext.packageName}.fileprovider"

    fun uriForFile(file: File): Uri {
        require(file.exists()) { "El archivo no existe: ${file.absolutePath}" }
        return FileProvider.getUriForFile(appContext, authority, file)
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
    ): ExportShareResult = shareInternal(context, file, mimeType, chooserTitle, preferWhatsApp = false)

    fun sharePdfWhatsApp(
        context: Context,
        file: File,
        message: String,
        chooserTitle: String = "Compartir comprobante",
    ): ExportShareResult {
        return when (val uriResult = uriOrFailure(file)) {
            null -> fileMissingFailure()
            is UriResult.Error -> uriResult.result
            is UriResult.Ready -> {
                val uri = uriResult.uri
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val whatsapp = Intent(send).apply { setPackage("com.whatsapp") }
                launchShare(context, whatsapp, send, chooserTitle)
            }
        }
    }

    fun openPdf(context: Context, file: File, chooserTitle: String = "Ver documento"): ExportShareResult {
        val uriResult = uriOrFailure(file) ?: return fileMissingFailure()
        val uri = (uriResult as UriResult.Ready).uri
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launchView(context, intent, chooserTitle)
    }

    fun openPdfUri(context: Context, uri: Uri, chooserTitle: String = "Ver documento"): ExportShareResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launchView(context, intent, chooserTitle)
    }

    private fun shareInternal(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        preferWhatsApp: Boolean,
    ): ExportShareResult {
        return when (val uriResult = uriOrFailure(file)) {
            null -> fileMissingFailure()
            is UriResult.Error -> uriResult.result
            is UriResult.Ready -> {
                val uri = uriResult.uri
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (preferWhatsApp) {
                    val whatsapp = Intent(send).apply { setPackage("com.whatsapp") }
                    launchShare(context, whatsapp, send, chooserTitle)
                } else {
                    launchShare(context, primary = send, fallback = send, chooserTitle = chooserTitle)
                }
            }
        }
    }

    private sealed class UriResult {
        data class Ready(val uri: Uri) : UriResult()
        data class Error(val result: ExportShareResult.Failure) : UriResult()
    }

    private fun uriOrFailure(file: File): UriResult? {
        if (!file.exists()) return null
        return try {
            UriResult.Ready(uriForFile(file))
        } catch (e: IllegalArgumentException) {
            UriResult.Error(ExportShareResult.Failure(userMessageFor(e), e))
        }
    }

    private fun fileMissingFailure() =
        ExportShareResult.Failure("No fue posible generar el archivo.")

    private fun launchShare(
        context: Context,
        primary: Intent,
        fallback: Intent,
        chooserTitle: String,
    ): ExportShareResult {
        return try {
            context.startActivity(primary)
            ExportShareResult.Success
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent.createChooser(fallback, chooserTitle))
                ExportShareResult.Success
            } catch (e: ActivityNotFoundException) {
                ExportShareResult.Failure("No existe una aplicación para compartir este documento.", e)
            } catch (e: SecurityException) {
                ExportShareResult.Failure("Permiso denegado para compartir el archivo.", e)
            }
        } catch (e: SecurityException) {
            ExportShareResult.Failure("Permiso denegado para compartir el archivo.", e)
        }
    }

    private fun launchView(context: Context, intent: Intent, chooserTitle: String): ExportShareResult {
        return try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            ExportShareResult.Success
        } catch (e: ActivityNotFoundException) {
            ExportShareResult.Failure("No existe una aplicación para abrir este documento.", e)
        } catch (e: SecurityException) {
            ExportShareResult.Failure("Permiso denegado para abrir el archivo.", e)
        }
    }

    private fun userMessageFor(error: Throwable): String = when (error) {
        is IllegalArgumentException -> "No fue posible preparar el archivo para compartir."
        is IOException -> "No fue posible guardar el archivo."
        is SecurityException -> "Permiso denegado."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "No fue posible completar la operación."
    }

    fun failureFrom(error: Throwable): ExportShareResult.Failure =
        ExportShareResult.Failure(userMessageFor(error), error)
}
