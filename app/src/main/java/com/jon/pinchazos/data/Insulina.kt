package com.jon.pinchazos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.floor
import kotlin.math.roundToLong

/** Una insulina que se pincha: su nombre, de cuánto en cuánto va la pluma y la dosis máxima. */
@Entity(tableName = "insulinas", indices = [Index(value = ["nombre"], unique = true)])
data class Insulina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** Lo que suman el + y el −: 0,5 (plumas de media unidad) o 1. */
    val paso: Double = 0.5,
    val maximo: Double = 12.0,
    /** Archivada: ya no sale para registrar, pero sus pinchazos se conservan en los informes. */
    val archivada: Boolean = false,
) {
    /** El mayor múltiplo de [paso] que no pasa de [maximo]. */
    val tope: Double get() = limpiar(floor(maximo / paso + 1e-9) * paso)

    /** Todos los valores elegibles, de 0 a [tope] en pasos de [paso]. */
    val valores: List<Double> get() = (0..(tope / paso).roundToLong()).map { limpiar(it * paso) }

    /**
     * Botones rápidos: todos los valores si caben en pocas filas (0; 0,5; … 12 en la de media
     * unidad); si no, el 25, 50, 75 y 100 % del máximo.
     */
    val atajos: List<Double>
        get() = valores.takeIf { it.size <= MAX_BOTONES }
            ?: listOf(0.25, 0.5, 0.75, 1.0).map { ajustar(tope * it) }.filter { it > 0 }.distinct()

    /** Redondea al paso más cercano y limita a [0, tope]. */
    fun ajustar(valor: Double): Double = limpiar((valor / paso).roundToLong() * paso).coerceIn(0.0, tope)

    fun sumarPasos(valor: Double, pasos: Int): Double = ajustar(valor + pasos * paso)

    companion object {
        /** La insulina con la que empezó la app: plumas de media unidad, hasta 12 u. */
        const val HUMALOG_JUNIOR = "Humalog Junior"

        /** Con la que arranca el selector si aún no se ha usado esa insulina. */
        const val DOSIS_POR_DEFECTO = 2.0

        const val MAX_BOTONES = 30

        /** Lo que pueden sumar el + y el −. */
        val PASOS = listOf(0.5, 1.0)

        fun humalogJunior() = Insulina(nombre = HUMALOG_JUNIOR, paso = 0.5, maximo = 12.0)

        /** Quita el ruido de coma flotante. */
        private fun limpiar(valor: Double): Double = Math.round(valor * 1e6) / 1e6
    }
}
