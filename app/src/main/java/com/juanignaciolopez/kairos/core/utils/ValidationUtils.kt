package com.juanignaciolopez.kairos.core.utils

/**
 * Utilidades para validación de datos
 */
object ValidationUtils {
    
    /**
     * Valida un email según el patrón estándar
     */
    fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }
    
    /**
     * Valida que una contraseña cumpla con los requisitos mínimos
     * - Al menos 8 caracteres
     * - Al menos una mayúscula
     * - Al menos un número
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() }
    }
    
    /**
     * Valida que un nombre no esté vacío y tenga al menos 2 caracteres
     */
    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }
    
    /**
     * Valida que un texto para una tarea no esté vacío
     */
    fun isValidTaskTitle(title: String): Boolean {
        return title.isNotBlank() && title.length <= 200
    }
    
    /**
     * Valida descripción de tarea (opcional pero max 2000 chars)
     */
    fun isValidTaskDescription(description: String): Boolean {
        return description.length <= 2000
    }
}
