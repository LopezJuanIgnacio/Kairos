package com.juanignaciolopez.kairos.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Modelo de usuario para autenticación y perfil.
 */
@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val emailVerified: Boolean = false
) {
    companion object {
        fun empty() = User(
            email = "",
            displayName = ""
        )
    }
}
