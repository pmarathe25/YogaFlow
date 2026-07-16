package com.example.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HeroPortrait(
    heroId: Int,
    elementColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    primaryColor: androidx.compose.ui.graphics.Color? = null,
    secondaryColor: androidx.compose.ui.graphics.Color? = null
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.6f
        val s = size.minDimension * 0.2f
        drawSilhouette(cx, cy, s, heroId, elementColor, primaryColor, secondaryColor)
    }
}
