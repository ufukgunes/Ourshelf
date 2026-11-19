package com.ourshelf.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourshelf.app.ui.motion.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

val StampInk = Color(0xFFE5533C)

/**
 * "FIN." mührü. [animate] true ise büyükten küçüğe yaylanarak "basılır".
 */
@Composable
fun FinStamp(animate: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(if (animate) 2.6f else 1f) }
    val alpha = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) {
            scale.snapTo(2.6f)
            alpha.snapTo(0f)
            launch { alpha.animateTo(1f, tween(120)) }
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                rotationZ = -14f
            }
            .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .border(3.dp, StampInk, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            "FIN.",
            color = StampInk,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = 4.sp,
        )
    }
}

/**
 * Basılı tutarak tamamlama. Dolum ilerledikçe titreşim güçlenir; [holdMillis] dolunca [onComplete] çağrılır.
 * Bırakılırsa geri sarılır. TalkBack kullanıcıları için tek dokunuşla tamamlayan bir semantik eylem sunar.
 */
@Composable
fun HoldToCompleteButton(
    text: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = 800,
) {
    val haptics = rememberHaptics()
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(progress) {
        snapshotFlow { (progress.value * RAMP_STEPS).toInt() }
            .distinctUntilChanged()
            .collect { step -> if (step in 1 until RAMP_STEPS) haptics.ramp(step / RAMP_STEPS.toFloat()) }
    }

    val fill = MaterialTheme.colorScheme.primary
    val onFill = MaterialTheme.colorScheme.onPrimary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val onTrack = MaterialTheme.colorScheme.onSurface

    Box(
        modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(track)
            .semantics {
                role = Role.Button
                onClick(label = text) {
                    haptics.confirm()
                    currentOnComplete()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val job = scope.launch {
                        val remaining = (holdMillis * (1f - progress.value)).toInt().coerceAtLeast(1)
                        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                        haptics.confirm()
                        currentOnComplete()
                        progress.snapTo(0f)
                    }
                    waitForUpOrCancellation()
                    if (job.isActive) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        HoldLabel(text, onTrack)
        // Dolum katmanı: aynı etiketin ters renkli kopyası, ilerleme genişliğine kırpılır.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() }
                }
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            HoldLabel(text, onFill)
        }
    }
}

@Composable
private fun HoldLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

private const val RAMP_STEPS = 8
