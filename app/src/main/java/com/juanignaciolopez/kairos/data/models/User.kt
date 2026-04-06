package com.juanignaciolopez.kairos.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Modelo de usuario para autenticación y perfil.
 */
@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val themePreference: String = "system",
    val language: String = "es"
) {
    companion object {
        fun empty() = User(
            email = "",
            displayName = ""
        )
    }
}

// Alias de transición para nombres en español.
typealias Usuario = User
