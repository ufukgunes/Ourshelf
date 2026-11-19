package com.ourshelf.app.ui.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

@Composable
actual fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    // Ayar uygulama açıkken değişebilir; her RESUMED'da yeniden oku.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return remember(lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** `View.performHapticFeedback` sistemin dokunma titreşimi ayarına da uyar. */
private class AndroidHaptics(private val view: View, private val vibrator: Vibrator?) : Haptics {
    override fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    /** Genlik kontrolü olan cihazlarda gerçek yoğunluk artışı, olmayanlarda sertleşen sistem efektleri. */
    override fun ramp(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        val v = vibrator
        if (v != null && v.hasVibrator() && v.hasAmplitudeControl()) {
            v.vibrate(VibrationEffect.createOneShot(18, (30 + f * 225).toInt().coerceIn(1, 255)))
        } else {
            view.performHapticFeedback(
                if (f < 0.5f) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.KEYBOARD_TAP,
            )
        }
    }
}

@Composable
actual fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    val enabled = LocalMotionSettings.current.hapticsEnabled
    return remember(view, enabled) { if (enabled) AndroidHaptics(view, context.vibrator()) else NoHaptics }
}

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }

@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    LifecycleResumeEffect(enabled) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = if (enabled) {
            manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            null
        }
        val filter = TiltFilter()
        val rotation = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val result = if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    // Dünya Z ekseninin cihaz koordinatlarındaki karşılığı = rotasyon matrisinin 3. satırı.
                    filter.update(rotation[6], rotation[7], rotation[8])
                } else {
                    filter.update(event.values[0], event.values[1], event.values[2])
                }
                result?.let { tilt.value = it }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose {
            manager?.unregisterListener(listener)
            tilt.value = Offset.Zero
        }
    }
    return tilt
}
