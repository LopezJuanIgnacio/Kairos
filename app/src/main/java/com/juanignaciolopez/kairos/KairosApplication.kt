package com.juanignaciolopez.kairos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KairosApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
