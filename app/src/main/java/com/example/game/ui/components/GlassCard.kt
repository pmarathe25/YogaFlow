package com.example.game.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ),
    border = CardDefaults.outlinedCardBorder().copy(
        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
) { Column(content = content) }
