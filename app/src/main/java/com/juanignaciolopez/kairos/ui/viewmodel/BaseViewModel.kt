package com.juanignaciolopez.kairos.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Clase base para todos los ViewModels del proyecto
 * Proporciona estructura común para manejo de estados y eventos
 * 
 * @param STATE Tipo de estado que maneja este ViewModel
 */

abstract class VistaModeloBase<ESTADO> : ViewModel() {
    
    // === MANEJO DE ESTADO ===
    // _estado es mutable internamente, pero se expone como inmutable (state)
    protected val _estado = MutableStateFlow<ESTADO>(obtenerEstadoInicial())
    // estado: flujo reactivo que emite cambios de estado a la UI
    val estado: StateFlow<ESTADO> = _estado.asStateFlow()
    
    /**
     * Retorna el estado inicial del ViewModel
     * Debe ser implementado por subclases con su estado específico
     */
    protected abstract fun obtenerEstadoInicial(): ESTADO
    
    /**
     * Actualiza el estado actual directamente
     * Método más simple para cambios directos
     */
    protected fun actualizarEstado(nuevoEstado: ESTADO) {
        _estado.value = nuevoEstado
    }
    
    /**
     * Actualiza el estado usando un bloque lambda
     * Permite cambios más complejos que dependen del estado anterior
     * Ejemplo: updateState { estado -> estado.copy(contador = estado.contador + 1) }
     */
    protected fun actualizarEstado(bloque: (ESTADO) -> ESTADO) {
        _estado.value = bloque(_estado.value)
    }
    
    /**
     * Obtain el estado actual de forma segura
     * Útil cuando necesitas el estado actual en operaciones síncronas
     */
    protected fun obtenerEstadoActual(): ESTADO = _estado.value
}

// Alias de transición para mantener compatibilidad con nombres previos.
typealias BaseViewModel<STATE> = VistaModeloBase<STATE>
