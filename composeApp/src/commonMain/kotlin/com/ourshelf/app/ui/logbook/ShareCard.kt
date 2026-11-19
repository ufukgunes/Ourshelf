package com.ourshelf.app.ui.logbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.ui.components.fallbackColors
import com.ourshelf.app.ui.components.rememberPosterModel

/**
 * Instagram hikâyesi oranında (9:16) paylaşım kartı: dönem başlığı, 3×3 poster kolajı, istatistik satırı ve filigran.
 * Tüm ölçüler kart genişliğine orantılıdır; böylece önizleme hangi boyutta çizilirse çizilsin aynı görünür.
 */
@Composable
fun ShareCard(
    periodLabel: String,
    subtitle: String,
    items: List<MediaItem>,
    statLines: List<String>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.aspectRatio(9f / 16f)) {
        val unit = maxWidth / 360f // 360 birimlik tasarım genişliği
        fun Float.u(): Dp = unit * this
        fun Float.s(): TextUnit = (unit.value * this).sp

        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1622), Color(0xFF3A2A4F), Color(0xFF6B4A2A)))),
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24f.u(), vertical = 28f.u()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "OURSHELF",
                    style = TextStyle(color = Color(0xFFE5A93C), fontSize = 11f.s(), letterSpacing = 3f.s(), fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(10f.u()))
                Text(
                    periodLabel,
                    style = TextStyle(color = Color.White, fontSize = 30f.s(), fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Text(
                    subtitle,
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 13f.s()),
                    textAlign = TextAlign.Center,
                )
                // Kolaj ve istatistikler dikeyde ortalanır; az eser olduğunda arada boşluk kalmaz.
                Spacer(Modifier.weight(1f))
                PosterGrid(items.take(9), gap = 8f.u(), corner = 8f.u())
                Spacer(Modifier.height(24f.u()))
                statLines.forEach { line ->
                    Text(
                        line,
                        style = TextStyle(color = Color.White, fontSize = 15f.s(), fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2f.u()),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Your shelf. Our shelf.  ·  Ourshelf",
                        style = TextStyle(color = Color.White.copy(alpha = 0.55f), fontSize = 10f.s()),
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterGrid(items: List<MediaItem>, gap: Dp, corner: Dp) {
    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    SharePoster(item, Modifier.weight(1f), corner)
                }
                // Eksik hücreler satırı sola yaslamasın diye boşlukla doldurulur.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Kart için her türü aynı 2:3 çerçevede gösterir (albüm kapakları kırpılır). */
@Composable
private fun SharePoster(item: MediaItem, modifier: Modifier, corner: Dp) {
    val (top, bottom) = fallbackColors(item.title)
    Box(
        modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            item.title,
            style = TextStyle(color = Color.White.copy(alpha = 0.9f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp).padding(4.dp),
        )
        val model = rememberPosterModel(item)
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
