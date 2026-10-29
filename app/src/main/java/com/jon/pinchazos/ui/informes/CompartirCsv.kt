package com.jon.pinchazos.ui.informes

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

object CompartirCsv {
    /** Tipos que aceptamos al importar; muchos gestores de archivos marcan los .csv de forma distinta. */
    val TIPOS_IMPORTABLES = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "text/plain",
        "application/csv",
        "application/vnd.ms-excel",
        "application/octet-stream",
    )

    /** Guarda el CSV en la caché y abre el menú de compartir de Android. */
    suspend fun compartir(contexto: Context, csv: String) {
        val archivo = withContext(Dispatchers.IO) {
            val carpeta = File(contexto.cacheDir, "exportes").apply { mkdirs() }
            File(carpeta, "pinchazos-${LocalDate.now()}.csv").apply { writeText(csv) }
        }
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Pinchazos de insulina")
            clipData = ClipData.newRawUri(archivo.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(envio, "Exportar CSV"))
    }
}
