package com.ourshelf.app.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.ourshelf.app.ui.motion.LocalMotionSettings

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Spesifikasyon: StiffnessMediumLow + DampingRatioLowBouncy — poster "süzülerek" yerine oturur. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val PosterBoundsTransform = BoundsTransform { _: Rect, _: Rect ->
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * Posteri ekranlar arası paylaşılan öğe yapar. Aynı ekranda bir eser birden fazla rafta görünebileceği için
 * anahtar, eserin hangi raftan açıldığını da içerir (örn. `lounge-current-<id>`); detay ekranı aynı anahtarı alır.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPoster(key: String?): Modifier {
    if (key == null || !LocalMotionSettings.current.animationsEnabled) return this
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sharedScope) {
        this@sharedPoster.sharedElement(
            sharedContentState = rememberSharedContentState(key = "poster-$key"),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = PosterBoundsTransform,
        )
    }
}
