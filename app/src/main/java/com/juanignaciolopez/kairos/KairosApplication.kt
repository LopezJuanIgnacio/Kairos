package com.juanignaciolopez.kairos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application de Kairos
 * Inicializa Hilt para inyección de dependencias
 */
@HiltAndroidApp
class KairosApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar librerías de terceros
    }
}
