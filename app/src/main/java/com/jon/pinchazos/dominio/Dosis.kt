package com.jon.pinchazos.dominio

import kotlin.math.roundToLong

object Dosis {
    const val MIN = 0.0
    const val MAX = 12.0
    const val PASO = 0.5
    const val POR_DEFECTO = 2.0

    /** 0; 0,5; 1; … 12 */
    val VALORES: List<Double> = (0..(MAX / PASO).toInt()).map { it * PASO }

    /** Redondea al paso de 0,5 más cercano y limita a [MIN, MAX]. */
    fun ajustar(valor: Double): Double = ((valor / PASO).roundToLong() * PASO).coerceIn(MIN, MAX)

    fun sumarPasos(valor: Double, pasos: Int): Double = ajustar(valor + pasos * PASO)
}
