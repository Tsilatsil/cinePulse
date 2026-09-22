package com.cinepulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Custom High-Fidelity Neon Logo recreating the user's provided dashboard emblem style.
 * Combines a neon rounded rectangle with canvas-drawn pulse wave paths and embedded movie reel icons.
 */
@Composable
fun CinePulseLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF0E0E18), shape = RoundedCornerShape(percent = 20))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F))
                ),
                shape = RoundedCornerShape(percent = 20)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(20f, size.height * 0.35f)
                lineTo(size.width * 0.45f, size.height * 0.35f)
                lineTo(size.width * 0.52f, size.height * 0.12f)
                lineTo(size.width * 0.60f, size.height * 0.65f)
                lineTo(size.width * 0.68f, size.height * 0.30f)
                lineTo(size.width * 0.75f, size.height * 0.35f)
                lineTo(size.width - 25f, size.height * 0.35f)
            }
            drawPath(
                path = path,
                color = Color(0xFFFF007F),
                style = Stroke(width = 5f)
            )
            drawCircle(
                color = Color(0xFFFF007F),
                radius = 7f,
                center = Offset(size.width - 22f, size.height * 0.35f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
                .size(64.dp)
                .background(Color(0xFF161626), shape = RoundedCornerShape(50))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
