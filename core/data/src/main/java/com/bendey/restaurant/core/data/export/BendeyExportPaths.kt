package com.bendey.restaurant.core.data.export

import android.content.Context
import java.io.File

/** Subdirectorios de cache usados para archivos compartibles vía FileProvider. */
object BendeyExportPaths {
    const val RECEIPTS = "receipts"
    const val EXPORTS = "exports"
    const val SUNAT_DOCS = "sunat-docs"

    fun receiptsDir(context: Context): File =
        File(context.cacheDir, RECEIPTS).also { it.mkdirs() }

    fun exportsDir(context: Context): File =
        File(context.cacheDir, EXPORTS).also { it.mkdirs() }

    fun sunatDocsDir(context: Context): File =
        File(context.cacheDir, SUNAT_DOCS).also { it.mkdirs() }

    fun exportFile(context: Context, relativePath: String): File {
        val file = File(context.cacheDir, relativePath)
        file.parentFile?.mkdirs()
        return file
    }
}
