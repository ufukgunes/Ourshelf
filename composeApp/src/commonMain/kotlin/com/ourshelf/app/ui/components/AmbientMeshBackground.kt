package com.ourshelf.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.ourshelf.app.ui.motion.LocalMotionSettings
import kotlin.math.cos
import kotlin.math.sin

/**
 * Posterin renklerinden üretilen, yavaşça "nefes alan" bulanık mesh gradient arka plan.
 * API 33+ cihazlarda AGSL shader, daha eskilerde aynı hareketi taklit eden radial gradient'ler kullanılır.
 * Alt kısma doğru tema arka planına karışan bir scrim, üstteki metnin okunabilirliğini korur.
 */
@Composable
fun AmbientMeshBackground(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    modifier: Modifier = Modifier,
    base: Color = MaterialTheme.colorScheme.background,
) {
    val dark = isSystemInDarkTheme()
    val intensity = if (dark) 0.85f else 0.5f
    val c1 by animateColorAsState(primary, tween(900), label = "ambient1")
    val c2 by animateColorAsState(secondary, tween(900), label = "ambient2")
    val c3 by animateColorAsState(tertiary, tween(900), label = "ambient3")
    val time = rememberAmbientTime(LocalMotionSettings.current.animationsEnabled)

    val shader = rememberMeshShader()

    Box(
        modifier.drawBehind {
            val t = time.value
            if (shader != null) {
                with(shader) { draw(t, c1, c2, c3, base, intensity) }
            } else {
                drawFallbackMesh(t, c1, c2, c3, base, intensity)
            }
            // Okunabilirlik scrim'i: üst kısım renkli, alt kısım sakin.
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to base.copy(alpha = 0.35f),
                    0.8f to base.copy(alpha = 0.85f),
                    1f to base,
                ),
            )
        },
    )
}

/** İki ana renkten üçüncü bir ton türetir (koyulaştırılmış karışım). */
fun ambientTertiary(a: Color, b: Color): Color = lerp(lerp(a, b, 0.5f), Color.Black, 0.45f)

/** Saniye cinsinden sürekli artan zaman; hareket kapalıysa sabit kalır (redraw tetiklenmez). */
@Composable
private fun rememberAmbientTime(animate: Boolean): State<Float> {
    if (!animate) return remember { mutableFloatStateOf(0f) }
    return produceState(0f) {
        var start = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { frame ->
                if (start < 0) start = frame
                value = (frame - start) / 1000f
            }
        }
    }
}

/** Blob merkezleri (0..1 uzayında). Shader ve fallback aynı yörüngeleri kullanır. */
private fun blobCenters(t: Float): Triple<Offset, Offset, Offset> = Triple(
    Offset(0.25f + 0.15f * sin(t * 0.55f), 0.18f + 0.10f * cos(t * 0.45f)),
    Offset(0.80f + 0.12f * cos(t * 0.40f), 0.32f + 0.14f * sin(t * 0.62f)),
    Offset(0.50f + 0.20f * sin(t * 0.30f + 1f), 0.62f + 0.10f * cos(t * 0.50f)),
)

private fun DrawScope.drawFallbackMesh(
    t: Float, c1: Color, c2: Color, c3: Color, base: Color, intensity: Float,
) {
    drawRect(base)
    val (p1, p2, p3) = blobCenters(t)
    val r = size.maxDimension * 0.55f
    listOf(p1 to c1, p2 to c2, p3 to c3).forEach { (p, c) ->
        val center = Offset(p.x * size.width, p.y * size.height)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(c.copy(alpha = intensity), c.copy(alpha = intensity * 0.4f), Color.Transparent),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
}

/** Platformun GPU shader'ı (Android: AGSL, API 33+; iOS: Skia RuntimeEffect). Yoksa/derlenemezse null → fallback. */
interface MeshShader {
    fun DrawScope.draw(t: Float, c1: Color, c2: Color, c3: Color, base: Color, intensity: Float)
}

@Composable
internal expect fun rememberMeshShader(): MeshShader?

/**
 * Üç hareketli gauss "ışık" lekesi taban rengin üzerine karıştırılır; banding'i önlemek için hafif dither eklenir.
 * Yörüngeler [blobCenters] ile birebir aynıdır.
 */
internal const val MESH_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float intensity;
uniform float4 c1;
uniform float4 c2;
uniform float4 c3;
uniform float4 base;

float blob(float2 uv, float2 p, float aspect) {
    float2 d = (uv - p) * float2(aspect, 1.0);
    return exp(-dot(d, d) * 3.2);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float t = iTime;

    float2 p1 = float2(0.25 + 0.15 * sin(t * 0.55), 0.18 + 0.10 * cos(t * 0.45));
    float2 p2 = float2(0.80 + 0.12 * cos(t * 0.40), 0.32 + 0.14 * sin(t * 0.62));
    float2 p3 = float2(0.50 + 0.20 * sin(t * 0.30 + 1.0), 0.62 + 0.10 * cos(t * 0.50));

    float3 col = base.rgb;
    col = mix(col, c1.rgb, blob(uv, p1, aspect) * intensity);
    col = mix(col, c2.rgb, blob(uv, p2, aspect) * intensity * 0.9);
    col = mix(col, c3.rgb, blob(uv, p3, aspect) * intensity * 0.8);

    float n = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
    col += (n - 0.5) / 255.0;

    return half4(half3(col), 1.0);
}
"""
