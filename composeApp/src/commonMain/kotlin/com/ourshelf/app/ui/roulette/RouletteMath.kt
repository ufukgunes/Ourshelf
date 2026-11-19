package com.ourshelf.app.ui.roulette

import com.ourshelf.app.core.model.LengthUnit
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.core.model.lengthUnit
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Çarkın durması gereken sürekli pozisyon. Karusel `position` değerinin tam sayı kısmındaki öğeyi ortada gösterir;
 * sonuç önceden seçilir ve animasyon tam olarak o öğede durur (adil ve deterministik).
 *
 * Hedef: `target ≥ current + minLoops * size` ve `target mod size == winner`.
 */
fun rouletteTarget(current: Float, size: Int, winner: Int, minLoops: Int): Float {
    require(size > 0) { "size must be positive" }
    require(winner in 0 until size) { "winner out of range" }
    val start = ceil(current.toDouble()).toLong() + minLoops.toLong() * size
    val offset = Math.floorMod(winner - start, size.toLong())
    return (start + offset).toFloat()
}

/** Pozisyondaki ortadaki öğenin indeksi. */
fun centeredIndex(position: Float, size: Int): Int =
    if (size == 0) 0 else Math.floorMod(floor(position.toDouble()).toLong(), size.toLong()).toInt()

/**
 * "Kısa olsun" filtresi: bir akşamda bitebilecek eserler.
 * Film ≤ 2 saat, kitap ≤ 250 sayfa, oyun ≤ 15 saat, dizi ≤ 10 bölüm; albüm ve podcast her zaman kısa.
 * Uzunluğu bilinmeyen film/kitap/oyun/dizi elenir (doğrulanamaz).
 */
fun isShortPick(item: MediaItem): Boolean {
    if (item.mediaType == MediaType.ALBUM || item.mediaType == MediaType.PODCAST) return true
    val length = item.lengthValue ?: return false
    return when (item.mediaType.lengthUnit) {
        LengthUnit.MINUTES -> length <= 120
        LengthUnit.PAGES -> length <= 250
        LengthUnit.HOURS -> length <= 15
        LengthUnit.EPISODES -> length <= 10
        LengthUnit.TRACKS -> true
    }
}

private object Math {
    fun floorMod(a: Long, b: Long): Long = ((a % b) + b) % b
}
