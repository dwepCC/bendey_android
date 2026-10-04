package com.bendey.restaurant.core.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.BulkImportProgress
import com.bendey.restaurant.core.domain.catalog.BulkImportReport
import com.bendey.restaurant.core.domain.catalog.BulkImportValidationResult
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.onboarding.wizard.importResultMessage
import java.io.ByteArrayOutputStream

/**
 * Importar la carta desde Excel (R4). Compartido por Productos y el wizard. Plantilla SIMPLE (4
 * columnas) primero; la de 11 columnas es la "avanzada". Se importan SOLO las filas válidas y se
 * muestra el resumen de omitidas; los errores (sin truncar) se guardan en un CSV.
 */
@Composable
fun ProductImportDialog(
    open: Boolean,
    validation: BulkImportValidationResult?,
    progress: BulkImportProgress?,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onFilePicked: (ByteArray) -> Unit,
    onImport: () -> Unit,
    onDownloadSimpleTemplate: () -> ByteArray,
    onDownloadAdvancedTemplate: () -> ByteArray,
    onDownloadError: (String) -> Unit = {},
) {
    if (!open) return
    val context = LocalContext.current
    val excelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readBytes(context, it)?.let(onFilePicked) }
    }
    fun save(uri: Uri?, bytes: () -> ByteArray) {
        uri ?: return
        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes()) }
                ?: error("No fue posible abrir el destino seleccionado.")
        }
        if (result.isFailure) onDownloadError("No fue posible guardar el archivo.")
    }
    val simpleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXCEL_MIME)) { uri ->
        save(uri, onDownloadSimpleTemplate)
    }
    val advancedLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXCEL_MIME)) { uri ->
        save(uri, onDownloadAdvancedTemplate)
    }
    val errorsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME)) { uri ->
        save(uri) { (validation?.let(BulkImportReport::errorsCsv) ?: "").toByteArray(Charsets.UTF_8) }
    }
    val failuresLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME)) { uri ->
        save(uri) { (progress?.let { BulkImportReport.failuresCsv(it.failed) } ?: "").toByteArray(Charsets.UTF_8) }
    }

    val canImport = !loading && progress == null && validation != null && validation.rows.isNotEmpty()

    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subir mi Excel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                Text(
                    "Usa la plantilla simple: nombre, categoría, precio y área. Si dejas el área vacía, el plato va a Cocina.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                BendeyPrimaryButton(
                    WizardCopy.EXCEL_DOWNLOAD_SIMPLE,
                    { simpleLauncher.launch("plantilla-simple-carta-bendey-resto.xlsx") },
                    modifier = Modifier.fillMaxWidth(),
                )
                BendeyOutlinedButton(
                    text = WizardCopy.EXCEL_DOWNLOAD_ADVANCED,
                    onClick = { advancedLauncher.launch("plantilla-productos-restaurante.xlsx") },
                    fillWidth = true,
                )
                BendeyOutlinedButton(
                    text = "Seleccionar archivo .xlsx",
                    onClick = { excelLauncher.launch(arrayOf(EXCEL_MIME, "application/vnd.ms-excel")) },
                    fillWidth = true,
                )
                validation?.let { result ->
                    Text(BulkImportReport.summary(result), fontWeight = FontWeight.SemiBold)
                    if (result.errors.isNotEmpty()) {
                        Text(
                            "Filas con errores (${result.errors.size}):",
                            color = BendeyColors.Error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        result.errors.take(5).forEach { err ->
                            Text(
                                "Fila ${err.row}: ${err.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.Error,
                            )
                        }
                        if (result.errors.size > 5) {
                            Text(
                                "…y ${result.errors.size - 5} más. Descarga el archivo para verlas todas.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        BendeyOutlinedButton(
                            text = "${WizardCopy.EXCEL_DOWNLOAD_ERRORS} (.csv)",
                            onClick = { errorsLauncher.launch("errores-importacion-bendey-resto.csv") },
                            fillWidth = true,
                        )
                    }
                }
                progress?.let {
                    Text(importResultMessage(it.created, it.failed.size), fontWeight = FontWeight.SemiBold)
                    it.failed.take(5).forEach { f ->
                        Text("Fila ${f.row}: ${f.message}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.Error)
                    }
                    if (it.failed.isNotEmpty()) {
                        BendeyOutlinedButton(
                            text = "${WizardCopy.EXCEL_DOWNLOAD_ERRORS} (.csv)",
                            onClick = { failuresLauncher.launch("errores-importacion-bendey-resto.csv") },
                            fillWidth = true,
                        )
                    }
                }
                error?.let { Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            if (progress == null) {
                BendeyPrimaryButton(
                    text = when {
                        loading -> "Importando…"
                        validation != null -> BulkImportReport.importButtonLabel(validation)
                        else -> "Importar"
                    },
                    onClick = onImport,
                    enabled = canImport,
                )
            } else {
                BendeyPrimaryButton(text = "Listo", onClick = onDismiss)
            }
        },
        dismissButton = { BendeyTextButton(text = "Cerrar", onClick = onDismiss) },
    )
}

private fun readBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
    }
}.getOrNull()

private const val EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val CSV_MIME = "text/csv"
