package com.juanignaciolopez.kairos.ui.viewmodel

/**
 * Interface que representa el contrato para un estado de UI
 * Todo estado debe poder ser replicado (para composición)
 * Las subclases implementan campos adicionales específicos del feature
 */
interface EstadoUI {
    val estaCargando: Boolean
    val error: String?
}

/**
 * Implementación base de EstadoUI
 * Proporciona los campos comunes: estaCargando y error
 */
data class EstadoUIBase(
    override val estaCargando: Boolean = false,
    override val error: String? = null
) : EstadoUI

/**
 * EXTENSIÓN: Copiar EstadoUI manteniendo los valores base
 * Transforma el estado actual con nuevo valor de estaCargando
 * Ejemplo: actualizarEstado { it.conCargando(true) }
 */
fun <T : EstadoUI> T.conCargando(estaCargando: Boolean): T {
    return when (this) {
        // Si es EstadoUIBase, usa copy() para crear nueva instancia con el nuevo valor
        is EstadoUIBase -> copy(estaCargando = estaCargando) as T
        // En otros casos, retorna sin cambios
        else -> this
    }
}

/**
 * EXTENSIÓN: Copiar EstadoUI con nuevo error
 * Actualiza el error del estado actual
 * Ejemplo: actualizarEstado { it.conError("Ocurrió un error") }
 */
fun <T : EstadoUI> T.conError(error: String?): T {
    return when (this) {
        // Si es EstadoUIBase, usa copy() para crear nueva instancia con el nuevo error
        is EstadoUIBase -> copy(error = error) as T
        // En otros casos, retorna sin cambios
        else -> this
    }
}

// Alias de transición para mantener compatibilidad con nombres previos.
typealias UiState = EstadoUI
typealias BaseUiState = EstadoUIBase

fun <T : UiState> T.withLoading(isLoading: Boolean): T =
    (this as EstadoUI).conCargando(isLoading) as T

fun <T : UiState> T.withError(error: String?): T =
    (this as EstadoUI).conError(error) as T
