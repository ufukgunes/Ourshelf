package com.ourshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.image.posterPath
import okio.FileSystem
import okio.SYSTEM
import kotlin.math.absoluteValue

/**
 * Poster / kapak. Görsel yoksa (veya yüklenene kadar) başlıktan türetilen gradient bir kapak gösterir;
 * böylece Mock veriler ve kapaksız kitaplar da rafta şık görünür.
 */
@Composable
fun Poster(
    title: String,
    type: MediaType,
    model: Any?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    showFallbackTitle: Boolean = true,
    sharedKey: String? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .sharedPoster(sharedKey)
            .aspectRatio(type.posterAspectRatio)
            .clip(shape)
            .background(fallbackBrush(title)),
    ) {
        if (showFallbackTitle) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Poster(
    item: MediaItem,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    sharedKey: String? = null,
) {
    Poster(
        title = item.title,
        type = item.mediaType,
        model = rememberPosterModel(item),
        modifier = modifier,
        cornerRadius = cornerRadius,
        sharedKey = sharedKey,
    )
}

/** Yerel önbellekteki poster varsa onu, yoksa uzak URL'yi kullanır. */
@Composable
fun rememberPosterModel(item: MediaItem): Any? = remember(item.localPosterFile, item.posterUrl) {
    item.localPosterFile
        ?.let(::posterPath)
        ?.takeIf { FileSystem.SYSTEM.exists(it) }
        ?: item.posterUrl
}

/** Başlığın hash'inden deterministik, koyu tonlu bir gradient. */
fun fallbackColors(seed: String): Pair<Color, Color> {
    val hue = (seed.hashCode().absoluteValue % 360).toFloat()
    val top = Color.hsv(hue, 0.42f, 0.50f)
    val bottom = Color.hsv((hue + 38f) % 360f, 0.55f, 0.22f)
    return top to bottom
}

private fun fallbackBrush(seed: String): Brush {
    val (top, bottom) = fallbackColors(seed)
    return Brush.linearGradient(listOf(top, bottom))
}
