package com.juanignaciolopez.kairos.core.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades para manejo de fechas y horas
 */
object DateUtils {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fullFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    /**
     * Convierte un timestamp a una fecha formateada
     */
    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))
    
    /**
     * Convierte un timestamp a una hora formateada
     */
    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))
    
    /**
     * Convierte un timestamp a fecha y hora completa
     */
    fun formatDateTime(millis: Long): String = fullFormat.format(Date(millis))
    
    /**
     * Retorna la fecha actual formateada
     */
    fun getCurrentDateFormatted(): String = formatDate(System.currentTimeMillis())
    
    /**
     * Retorna true si la fecha es hoy
     */
    fun isToday(millis: Long): Boolean {
        val today = java.util.Calendar.getInstance().apply {
            time = Date(System.currentTimeMillis())
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val date = java.util.Calendar.getInstance().apply {
            time = Date(millis)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        return today.timeInMillis == date.timeInMillis
    }
}
