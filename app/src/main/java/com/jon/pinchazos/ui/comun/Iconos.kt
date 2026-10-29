package com.jon.pinchazos.ui.comun

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Iconos propios para no depender de material-icons. */
object Iconos {
    val Gota: ImageVector by lazy {
        ImageVector.Builder("Gota", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2.5f)
                curveTo(12f, 2.5f, 5f, 10.2f, 5f, 14.5f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 14.5f)
                curveTo(19f, 10.2f, 12f, 2.5f, 12f, 2.5f)
                close()
            }
        }.build()
    }

    val Barras: ImageVector by lazy {
        ImageVector.Builder("Barras", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                barra(4f, 12f)
                barra(10f, 5f)
                barra(16f, 9f)
            }
        }.build()
    }

    val Papelera: ImageVector by lazy {
        ImageVector.Builder("Papelera", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                // Tapa con asa
                moveTo(9f, 3f)
                lineTo(15f, 3f)
                lineTo(15f, 4.5f)
                lineTo(19f, 4.5f)
                lineTo(19f, 6.5f)
                lineTo(5f, 6.5f)
                lineTo(5f, 4.5f)
                lineTo(9f, 4.5f)
                close()
                // Cubo
                moveTo(6.5f, 8f)
                lineTo(17.5f, 8f)
                lineTo(16.6f, 19.2f)
                quadTo(16.5f, 21f, 14.8f, 21f)
                lineTo(9.2f, 21f)
                quadTo(7.5f, 21f, 7.4f, 19.2f)
                close()
            }
        }.build()
    }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.barra(x: Float, arriba: Float) {
        moveTo(x, 20f)
        lineTo(x, arriba + 1f)
        quadTo(x, arriba, x + 1f, arriba)
        lineTo(x + 3f, arriba)
        quadTo(x + 4f, arriba, x + 4f, arriba + 1f)
        lineTo(x + 4f, 20f)
        close()
    }
}
