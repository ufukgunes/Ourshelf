package com.ourshelf.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.ourshelf.app.ui.motion.LocalMotionSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFFE5A93C), Color(0xFFE5533C), Color(0xFF6E5BA8), Color(0xFF4FB3A9), Color(0xFFF4EDE4),
)

private class Particle(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
)

/**
 * Ortadan dışarı saçılan konfeti patlaması. [trigger] her değiştiğinde yeniden oynar.
 * Hareket efektleri kapalıysa hiçbir şey çizmez.
 */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (!LocalMotionSettings.current.animationsEnabled || trigger == 0) return
    val particles = remember(trigger) {
        val random = Random(trigger)
        List(70) {
            Particle(
                angle = (random.nextFloat() * 2 * PI).toFloat(),
                speed = 0.35f + random.nextFloat() * 0.65f,
                spin = random.nextFloat() * 720f - 360f,
                size = 6f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(1600, easing = LinearEasing)) }

    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val origin = Offset(size.width / 2, size.height * 0.42f)
        val reach = size.minDimension * 0.75f
        particles.forEach { p ->
            // Dışarı doğru yavaşlayan itki + aşağı doğru yerçekimi.
            val travel = reach * p.speed * (1 - (1 - t) * (1 - t))
            val gravity = size.height * 0.45f * t * t
            val center = Offset(
                origin.x + cos(p.angle) * travel,
                origin.y + sin(p.angle) * travel + gravity,
            )
            rotate(p.spin * t, pivot = center) {
                drawRect(
                    color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                    topLeft = Offset(center.x - p.size / 2, center.y - p.size / 4),
                    size = Size(p.size, p.size / 2),
                )
            }
        }
    }
}
