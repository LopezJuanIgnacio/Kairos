package com.juanignaciolopez.kairos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun CircleIcon(
    icon: @Composable () -> Unit,
    hasShadow: Boolean = true
) {
    val amber = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(112.dp)
            .then(
                if (hasShadow) {
                    Modifier.shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        ambientColor = amber.copy(alpha = 0.7f),
                        spotColor = amber.copy(alpha = 0.9f)
                    )
                } else {
                    Modifier
                }
            )
            .background(amber, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
