package com.juanignaciolopez.kairos.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircleIcon(
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    hasShadow: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    val amber = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
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
            .background(amber, CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
