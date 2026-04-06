package com.juanignaciolopez.kairos.ui.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Interface base para eventos del UI
 * Todos los eventos deben implementar esta interfaz
 */
interface EventoUI

/**
 * Clase base para ViewModel que necesita manejar eventos de UI
 * Extiende BaseViewModel agregando manejo de eventos
 * 
 * @param STATE Tipo de estado
 * @param EVENT Tipo de evento
 */
abstract class VistaModeloBaseConEventos<ESTADO, EVENTO : EventoUI> :
    VistaModeloBase<ESTADO>() {
    
    // === MANEJO DE EVENTOS ===
    // _evento es mutable internamente, pero se expone como inmutable (evento)
    // Usa SharedFlow para soportar múltiples coleccionadores de eventos
    protected val _evento = MutableSharedFlow<EVENTO>()
    // evento: flujo reactivo donde se emiten eventos para ser procesados por la UI
    val evento: SharedFlow<EVENTO> = _evento.asSharedFlow()
    
    /**
     * Emite un evento que será procesado por la UI
     * Típicamente usado para navegación, mostrar diálogos, etc
     */
    protected suspend fun enviarEvento(evento: EVENTO) {
        _evento.emit(evento)
    }
}

/**
 * Eventos comunes de UI
 */
sealed class EventoUIComun : EventoUI {
    // Mostrar mensaje temporal (toast)
    data class MostrarToast(val mensaje: String) : EventoUIComun()
    // Mostrar diálogo modal
    data class MostrarDialogo(val titulo: String, val mensaje: String) : EventoUIComun()
    // Navegar hacia atrás en la pila
    data object VolverAtras : EventoUIComun()
    // Navegar a una ruta específica
    data class NavegarA(val ruta: String) : EventoUIComun()
}

// Alias de transición para mantener compatibilidad con nombres previos.
typealias UiEvent = EventoUI
typealias CommonUiEvent = EventoUIComun
typealias BaseEventViewModel<STATE, EVENT> = VistaModeloBaseConEventos<STATE, EVENT>
