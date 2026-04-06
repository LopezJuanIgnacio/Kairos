package com.juanignaciolopez.kairos.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Componente de carga circular reutilizable
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Componente de estado de carga en línea (más pequeño)
 */
@Composable
fun InlineLoadingIndicator(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(modifier = modifier)
}

// Alias en español para uso progresivo en el resto del proyecto.
@Composable
fun IndicadorCarga(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier)
}

@Composable
fun IndicadorCargaEnLinea(modifier: Modifier = Modifier) {
    InlineLoadingIndicator(modifier = modifier)
}
