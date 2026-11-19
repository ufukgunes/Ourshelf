package com.ourshelf.app.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Uygulama genelindeki hareket/haptik tercihleri. Kullanıcı tercihi + sistemin
 * "animasyonları kaldır / hareketi azalt" ayarı birleştirilerek hesaplanır.
 */
data class MotionSettings(
    /** Ambiyans "nefes alma", cover-flow eğimi vb. süs animasyonları. */
    val animationsEnabled: Boolean = true,
    /** Sensörle 3B poster eğimi. */
    val tiltEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
)

val LocalMotionSettings = compositionLocalOf { MotionSettings() }

/** Sistem ayarlarında animasyonlar kapatılmış mı? (Android: animatör ölçeği 0, iOS: Hareketi Azalt.) */
@Composable
expect fun rememberSystemAnimationsEnabled(): Boolean

/** Dokunsal geri bildirim. Tüm çağrılar kullanıcı tercihine saygı duyar. */
interface Haptics {
    /** Çark/karusel tıkırtısı. */
    fun tick()

    /** İşlem başarıyla tamamlandı. */
    fun confirm()

    /** Giderek güçlenen titreşim (0..1). */
    fun ramp(fraction: Float)
}

object NoHaptics : Haptics {
    override fun tick() = Unit
    override fun confirm() = Unit
    override fun ramp(fraction: Float) = Unit
}

@Composable
expect fun rememberHaptics(): Haptics

/**
 * Cihazın başlangıç duruşuna göre eğimi (derece; x -> rotationY, y -> rotationX).
 * Yalnızca ekran görünür ve RESUMED iken dinlenir.
 */
@Composable
expect fun rememberDeviceTilt(enabled: Boolean): State<Offset>

const val MAX_TILT_DEGREES = 12f

/**
 * Platformdan gelen yerçekimi ("yukarı") vektörünü poster eğimine çevirir. Android ve iOS aynı hesabı kullanır.
 *
 * Sağ-sol eğim = asin(gx), ileri-geri eğim = atan2(gy, gz). Bu yöntem telefon dik tutulurken de kararlıdır.
 * Referans duruş yavaşça yeniden ortalanır; titreşim düşük geçiren filtreyle yumuşatılır.
 */
class TiltFilter {
    private var baseline: Offset? = null
    private var filtered = Offset.Zero

    fun reset() {
        baseline = null
        filtered = Offset.Zero
    }

    fun update(gx: Float, gy: Float, gz: Float): Offset? {
        val norm = sqrt(gx * gx + gy * gy + gz * gz)
        if (norm < 1e-3f) return null
        val x = gx / norm
        val y = gy / norm
        val z = gz / norm
        val current = Offset(
            degrees(asin(x.coerceIn(-1f, 1f).toDouble())),
            degrees(atan2(y.toDouble(), z.toDouble())),
        )
        val base = baseline?.let { it + (current - it) * RECENTER_RATE } ?: current
        baseline = base

        val delta = Offset(wrap(current.x - base.x), wrap(current.y - base.y))
        val target = Offset(
            (-delta.x * GAIN).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES),
            (delta.y * GAIN).coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES),
        )
        filtered += (target - filtered) * LOW_PASS
        return filtered
    }

    private fun degrees(rad: Double): Float = (rad * 180.0 / PI).toFloat()

    private fun wrap(deg: Float): Float = when {
        deg > 180f -> deg - 360f
        deg < -180f -> deg + 360f
        else -> deg
    }

    private companion object {
        const val GAIN = 1.2f
        const val LOW_PASS = 0.15f
        const val RECENTER_RATE = 0.01f
    }
}
