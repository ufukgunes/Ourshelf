package com.ourshelf.app.data.image

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Posterden ambiyans renklerini çıkaran, platformdan bağımsız küçük bir renk nicemleyici.
 *
 * Pikseller kanal başına 4 bit'e (4096 kova) indirgenir:
 * - **Baskın renk:** en kalabalık kovanın ortalaması.
 * - **Vurgu rengi:** yeterince temsil edilen (≥%1) kovalar arasında en "canlı" olanı
 *   (doygunluk × parlaklık, nüfusla hafifçe ağırlıklandırılmış). Uygun aday yoksa null.
 */
object ColorExtractor {

    data class Colors(val dominant: Int?, val accent: Int?)

    fun extract(argbPixels: IntArray): Colors {
        val counts = IntArray(BUCKETS)
        val sumR = LongArray(BUCKETS)
        val sumG = LongArray(BUCKETS)
        val sumB = LongArray(BUCKETS)
        var total = 0

        for (p in argbPixels) {
            if ((p ushr 24) < 128) continue // saydam pikselleri yok say
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val idx = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            counts[idx]++
            sumR[idx] += r.toLong(); sumG[idx] += g.toLong(); sumB[idx] += b.toLong()
            total++
        }
        if (total == 0) return Colors(null, null)

        fun average(idx: Int): Int {
            val n = counts[idx]
            return argb((sumR[idx] / n).toInt(), (sumG[idx] / n).toInt(), (sumB[idx] / n).toInt())
        }

        val dominantIdx = counts.indices.maxBy { counts[it] }
        val minPopulation = max(1, total / 100)

        var bestScore = 0.0
        var accentIdx = -1
        for (i in 0 until BUCKETS) {
            val n = counts[i]
            if (n < minPopulation) continue
            val (s, v) = saturationValue(average(i))
            if (s < MIN_SATURATION || v < MIN_VALUE || v > MAX_VALUE) continue
            val score = s * v * ln(1.0 + n)
            if (score > bestScore) {
                bestScore = score
                accentIdx = i
            }
        }

        return Colors(
            dominant = average(dominantIdx),
            accent = if (accentIdx >= 0) average(accentIdx) else null,
        )
    }

    internal fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** HSV'nin S ve V bileşenleri (0..1). */
    internal fun saturationValue(color: Int): Pair<Double, Double> {
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val s = if (maxC == 0.0) 0.0 else (maxC - minC) / maxC
        return s to maxC
    }

    private const val BUCKETS = 4096
    private const val MIN_SATURATION = 0.35
    private const val MIN_VALUE = 0.3
    private const val MAX_VALUE = 0.98
}
