package com.jon.pinchazos.ui.comun

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * El movimiento de la app, en un sitio. Criterio: lo que se ve muchas veces al día (cambiar de
 * pestaña, pulsar) es rápido y discreto. Entradas y salidas con ease-out fuerte, nunca ease-in;
 * nada pasa de ~300 ms salvo los muelles. Compose ya respeta "Quitar animaciones" del sistema.
 */
object Movimiento {
    /** Ease-out fuerte: arranca rápido y se posa suave. Para entradas, salidas y respuestas. */
    val Salida = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Ease-in-out fuerte, para lo que se mueve de un sitio a otro en pantalla. */
    val Desplazamiento = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    fun <T> rapido(ms: Int = 200) = tween<T>(durationMillis = ms, easing = Salida)

    /** Muelle sin rebote apenas: para indicadores y barras que siguen al dedo o a un dato. */
    fun <T> muelle() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

    /** Muelle con un pelín de rebote: para el "¡hecho!" de un check. */
    fun <T> pop() = spring<T>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
}

/**
 * Aparece subiendo un poco y desvaneciéndose, con un retraso según [indice] (30–40 ms entre
 * elementos, como mucho 8 escalones) para que una lista entre en cascada. Solo la primera vez que
 * se compone (o cuando cambia [clave]), no en cada recomposición.
 */
@Composable
fun Modifier.entradaEscalonada(indice: Int, clave: Any? = Unit): Modifier {
    // En una lista larga, un elemento sale y vuelve a entrar al desplazarse: si cada vez repitiera
    // la entrada, la lista "rebota" al subir y bajar. Se guarda que ya entró (rememberSaveable
    // sobrevive a que la lista lo saque de pantalla) y entonces aparece quieto.
    var entrado by rememberSaveable(clave?.toString()) { mutableStateOf(false) }
    val progreso = remember(clave) { Animatable(if (entrado) 1f else 0f) }
    LaunchedEffect(clave) {
        if (entrado) return@LaunchedEffect
        entrado = true
        progreso.animateTo(1f, tween(durationMillis = 360, delayMillis = indice.coerceAtMost(8) * 35, easing = Movimiento.Salida))
    }
    return graphicsLayer {
        val p = progreso.value
        alpha = p
        translationY = (1f - p) * 14.dp.toPx()
    }
}

/** Texto que, al cambiar, sale hacia arriba y entra el nuevo desde abajo (contadores, horas). */
@Composable
fun TextoCambiante(texto: String, estilo: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    AnimatedContent(
        targetState = texto,
        transitionSpec = {
            (slideInVertically(Movimiento.rapido(260)) { it / 2 } + fadeIn(Movimiento.rapido(260))) togetherWith
                (slideOutVertically(Movimiento.rapido(180)) { -it / 2 } + fadeOut(Movimiento.rapido(140)))
        },
        modifier = modifier,
        label = "textoCambiante",
    ) { t -> Text(t, style = estilo, color = color) }
}
