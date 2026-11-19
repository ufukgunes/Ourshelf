package com.ourshelf.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.ui.motion.LocalMotionSettings
import com.ourshelf.app.ui.motion.MAX_TILT_DEGREES
import com.ourshelf.app.ui.motion.rememberDeviceTilt
import kotlinx.coroutines.launch

/**
 * Cihaz eğimine tepki veren 3B "koleksiyon kartı" poster: ±12° döner, üzerinde ışık yansıması gezinir.
 * Sensör yoksa (veya emülatörde) posteri parmakla sürükleyerek aynı efekt elde edilir; bırakınca yaylanarak düzelir.
 */
@Composable
fun TactilePoster(
    item: MediaItem,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val motion = LocalMotionSettings.current
    val sensorTilt = rememberDeviceTilt(enabled = motion.tiltEnabled)
    val touchTilt = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(14.dp)

    // Derece cinsinden: x -> rotationY, y -> rotationX.
    fun currentTilt(): Offset {
        val t = sensorTilt.value + touchTilt.value
        return Offset(t.x.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES), t.y.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES))
    }

    Box(
        modifier
            .sharedPoster(sharedKey)
            .pointerInput(motion.tiltEnabled) {
                if (!motion.tiltEnabled) return@pointerInput
                val release: () -> Unit = {
                    scope.launch {
                        touchTilt.animateTo(
                            Offset.Zero,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        )
                    }
                }
                detectDragGestures(
                    onDragEnd = release,
                    onDragCancel = release,
                ) { change, drag ->
                    change.consume()
                    val next = touchTilt.value + Offset(
                        drag.x / size.width * MAX_TILT_DEGREES * 2,
                        -drag.y / size.height * MAX_TILT_DEGREES * 2,
                    )
                    scope.launch {
                        touchTilt.snapTo(
                            Offset(
                                next.x.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES),
                                next.y.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES),
                            ),
                        )
                    }
                }
            }
            .graphicsLayer {
                val t = currentTilt()
                rotationY = t.x
                rotationX = t.y
                cameraDistance = 14f * density
                shadowElevation = 22.dp.toPx()
                this.shape = shape
                clip = false
            },
    ) {
        Poster(item, Modifier.fillMaxWidth(), cornerRadius = 14.dp)
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .drawWithContent {
                    drawContent()
                    val t = currentTilt() / MAX_TILT_DEGREES // -1..1
                    // Yuvarlak speküler parlama: eğimin tersine kayar.
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(size.width * (0.5f - t.x * 0.55f), size.height * (0.32f + t.y * 0.45f)),
                            radius = size.maxDimension * 0.65f,
                        ),
                    )
                    // Kuşe kağıt "sheen" bandı.
                    val band = (0.5f - t.x * 0.4f + t.y * 0.2f).coerceIn(0.2f, 0.8f)
                    drawRect(
                        Brush.linearGradient(
                            0f to Color.Transparent,
                            band - 0.18f to Color.Transparent,
                            band to Color.White.copy(alpha = 0.10f),
                            band + 0.18f to Color.Transparent,
                            1f to Color.Transparent,
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )
                },
        )
        overlay()
    }
}
