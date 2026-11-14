package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.cyberCard(
    cornerRadius: Dp = 24.dp,
    borderColor: Color = CyberSurfaceBorder,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = CyberSurfaceGlass
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val sheen = Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color(0x04FFFFFF)))
    return this
        .clip(shape)
        .background(backgroundColor, shape)
        .background(sheen, shape)
        .border(borderWidth, borderColor, shape)
}

fun Modifier.neonCard(
    cornerRadius: Dp = 16.dp,
    glowColor: Color = NeonCyan
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val gradient = Brush.linearGradient(
        colors = listOf(
            glowColor.copy(alpha = 0.6f),
            glowColor.copy(alpha = 0.1f),
            NeonPurple.copy(alpha = 0.3f)
        )
    )
    return this
        .clip(shape)
        .background(CyberSurfaceGlass, shape)
        .border(1.5.dp, gradient, shape)
}

val HologramRaysGradient = Brush.verticalGradient(
    colors = listOf(
        NeonCyan.copy(alpha = 0.35f),
        NeonBlue.copy(alpha = 0.15f),
        Color.Transparent
    )
)

val PedestalGlowGradient = Brush.radialGradient(
    colors = listOf(
        NeonCyan.copy(alpha = 0.8f),
        NeonPurple.copy(alpha = 0.4f),
        Color.Transparent
    )
)

val CyberButtonGradient = Brush.horizontalGradient(
    colors = listOf(
        NeonCyan,
        NeonBlue
    )
)

val PersonaPillGradient = Brush.horizontalGradient(
    colors = listOf(
        NeonPurple,
        NeonViolet
    )
)

/** Soft dark gradient used behind every screen. */
val AppBackgroundBrush = Brush.verticalGradient(
    colors = listOf(Color(0xFF0D1424), Color(0xFF080C14), Color(0xFF0A0F1C))
)

/**
 * Glowing neon card like the reference design: dark glass, coloured gradient outline and a soft glow
 * (the coloured glow is visible on Android 9+, older phones show a normal shadow).
 */
fun Modifier.neonCard(
    color: Color = NeonCyan,
    cornerRadius: Dp = 24.dp,
    glow: Dp = 10.dp,
    fill: Color = Color(0xD00A1220)
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(elevation = glow, shape = shape, clip = false, ambientColor = color, spotColor = color)
        .clip(shape)
        .background(fill, shape)
        .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.12f), Color.Transparent)), shape)
        .border(
            width = 1.5.dp,
            brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.35f), color)),
            shape = shape
        )
}

/** Dark teal background with faint circuit-board traces (like the reference screenshot). */
fun Modifier.circuitBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xFF03181F), Color(0xFF050B14), Color(0xFF07111D))))
    .drawWithCache {
        val step = 34.dp.toPx()
        val cols = (size.width / step).toInt() + 1
        val rows = (size.height / step).toInt() + 1
        val rnd = java.util.Random(42)
        val traces = Path()
        val dots = ArrayList<Offset>()
        repeat(60) {
            var x = rnd.nextInt(cols) * step
            var y = rnd.nextInt(rows) * step
            traces.moveTo(x, y)
            dots.add(Offset(x, y))
            repeat(3 + rnd.nextInt(3)) {
                if (rnd.nextBoolean()) x += (rnd.nextInt(5) - 2) * step else y += (rnd.nextInt(5) - 2) * step
                traces.lineTo(x, y)
            }
            dots.add(Offset(x, y))
        }
        val lineColor = Color(0x2400E5FF)
        val dotColor = Color(0x5522D3EE)
        val lineWidth = 1.dp.toPx()
        val dotRadius = 2.5.dp.toPx()
        onDrawBehind {
            drawPath(traces, lineColor, style = Stroke(width = lineWidth))
            for (d in dots) drawCircle(dotColor, dotRadius, d)
        }
    }
