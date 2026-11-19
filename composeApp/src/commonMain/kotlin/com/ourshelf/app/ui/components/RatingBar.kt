package com.ourshelf.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Yarım yıldız destekli 5'li puanlama. Her yıldızın sol yarısına dokunmak x.5, sağ yarısı tam puan verir.
 * Mevcut puana tekrar dokunmak puanı kaldırır.
 */
@Composable
fun RatingBar(
    rating: Float?,
    onRatingChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp,
) {
    val value = rating ?: 0f
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.semantics { contentDescription = "Rating ${rating ?: 0f} / 5" },
    ) {
        for (i in 1..5) {
            val icon = when {
                value >= i -> Icons.Filled.Star
                value >= i - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Box(Modifier.size(starSize)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(starSize))
                Row {
                    listOf(i - 0.5f, i.toFloat()).forEach { target ->
                        Box(
                            Modifier
                                .size(width = starSize / 2, height = starSize)
                                .fillMaxHeight()
                                .clickable { onRatingChange(if (target == rating) null else target) },
                        )
                    }
                }
            }
        }
    }
}
