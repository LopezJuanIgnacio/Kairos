package com.juanignaciolopez.kairos.data.models

/**
 * Resultado de operaciones asíncronas.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun exceptionOrNull(): Exception? = when (this) {
        is Error -> exception
        else -> null
    }
}

// Alias de transición para nombres en español.
typealias Resultado<T> = Result<T>

inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Result.Success -> Result.Success(transformar(data))
    is Result.Error -> this
    is Result.Loading -> this
}

inline fun <T> Resultado<T>.alExito(accion: (T) -> Unit): Resultado<T> {
    if (this is Result.Success) {
        accion(data)
    }
    return this
}

inline fun <T> Resultado<T>.alError(accion: (String, Exception?) -> Unit): Resultado<T> {
    if (this is Result.Error) {
        accion(message, exception)
    }
    return this
}
