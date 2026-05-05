package com.juanignaciolopez.kairos.core.preferences

import android.content.Context
//Guardar el estado del onboarding
class OnboardingPreferences(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun markCompleted() {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "kairos_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}