package com.juanignaciolopez.kairos.core.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Diálogo de error reutilizable
 * 
 * @param onDismiss Callback cuando se cierra el diálogo
 * @param title Título del error
 * @param message Mensaje de error
 * @param actionText Texto del botón de acción (por defecto "OK")
 */
@Composable
fun ErrorDialog(
    onDismiss: () -> Unit,
    title: String = "Error",
    message: String,
    actionText: String = "OK"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(actionText)
            }
        }
    )
}

/**
 * Diálogo de confirmación reutilizable
 */
@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun DialogoError(
    alCerrar: () -> Unit,
    titulo: String = "Error",
    mensaje: String,
    textoAccion: String = "OK"
) {
    ErrorDialog(
        onDismiss = alCerrar,
        title = titulo,
        message = mensaje,
        actionText = textoAccion
    )
}

@Composable
fun DialogoConfirmacion(
    alCerrar: () -> Unit,
    alConfirmar: () -> Unit,
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    textoCancelar: String = "Cancelar"
) {
    ConfirmationDialog(
        onDismiss = alCerrar,
        onConfirm = alConfirmar,
        title = titulo,
        message = mensaje,
        confirmText = textoConfirmar,
        dismissText = textoCancelar
    )
}
