package com.ourshelf.app.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

@Composable
actual fun rememberSystemAnimationsEnabled(): Boolean = remember { !UIAccessibilityIsReduceMotionEnabled() }

private class IosHaptics : Haptics {
    private val selection = UISelectionFeedbackGenerator()
    private val impact = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val notification = UINotificationFeedbackGenerator()

    override fun tick() = selection.selectionChanged()

    override fun confirm() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

    override fun ramp(fraction: Float) =
        impact.impactOccurredWithIntensity(fraction.toDouble().coerceIn(0.1, 1.0))
}

@Composable
actual fun rememberHaptics(): Haptics {
    val enabled = LocalMotionSettings.current.hapticsEnabled
    return remember(enabled) { if (enabled) IosHaptics() else NoHaptics }
}

/**
 * CoreMotion'ın `gravity` vektörü aşağıyı gösterir; Android ivmeölçeriyle aynı "yukarı" vektörünü elde etmek için
 * işareti çevrilir. Eksen yönleri (x sağ, y üst, z ekrandan dışarı) Android ile aynıdır.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Offset> {
    val tilt = remember { mutableStateOf(Offset.Zero) }
    LifecycleResumeEffect(enabled) {
        val manager = CMMotionManager()
        val filter = TiltFilter()
        if (enabled && manager.deviceMotionAvailable) {
            manager.deviceMotionUpdateInterval = 1.0 / 60.0
            manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
                motion?.gravity?.useContents {
                    filter.update(-x.toFloat(), -y.toFloat(), -z.toFloat())?.let { tilt.value = it }
                }
            }
        }
        onPauseOrDispose {
            manager.stopDeviceMotionUpdates()
            tilt.value = Offset.Zero
        }
    }
    return tilt
}
