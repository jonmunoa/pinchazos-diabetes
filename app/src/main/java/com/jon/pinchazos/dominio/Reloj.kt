package com.jon.pinchazos.dominio

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emite la hora actual al suscribirse y luego cada [intervaloMs]. */
fun relojCada(intervaloMs: Long): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(intervaloMs)
    }
}
