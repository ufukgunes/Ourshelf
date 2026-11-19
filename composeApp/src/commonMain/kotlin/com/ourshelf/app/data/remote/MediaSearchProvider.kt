package com.ourshelf.app.data.remote

import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tüm medya sağlayıcılarının ortak arayüzü (Adapter Pattern).
 */
interface MediaSearchProvider {
    /** Loglarda / hata mesajlarında görünen ad. */
    val name: String

    val supportedTypes: Set<MediaType>

    suspend fun search(query: String, type: MediaType, page: Int = 1): Result<List<MediaSearchResult>>

    /**
     * Arama sonucunu detay bilgileriyle zenginleştirir (süre, tür, yönetmen, platformlar...).
     * Sağlayıcının detay endpoint'i yoksa sonucu aynen döndürür.
     */
    suspend fun details(result: MediaSearchResult): Result<MediaSearchResult> = Result.success(result)
}

/** `runCatching` gibi, ama coroutine iptalini yutmaz. */
internal inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/** "2010-07-16" -> 2010 */
internal fun String?.yearOrNull(): Int? = this?.take(4)?.toIntOrNull()

/**
 * Aksanları ve Türkçe özel harfleri ASCII karşılığına indirger ("Kürk Işığı" -> "Kurk Isigi").
 * Ortak kodda `java.text.Normalizer` olmadığı için sık kullanılan Latin harfleri elle eşlenir.
 */
internal fun foldDiacritics(text: String): String = buildString(text.length) {
    for (c in text) append(DIACRITIC_FOLDS[c] ?: c)
}

private val DIACRITIC_FOLDS: Map<Char, Char> = buildMap {
    fun map(from: String, to: Char) = from.forEach { put(it, to) }
    map("àáâãäåāăą", 'a'); map("ÀÁÂÃÄÅĀĂĄ", 'A')
    map("çćĉċč", 'c'); map("ÇĆĈĊČ", 'C')
    map("èéêëēĕėęě", 'e'); map("ÈÉÊËĒĔĖĘĚ", 'E')
    map("ğĝġģ", 'g'); map("ĞĜĠĢ", 'G')
    map("ìíîïĩīĭįı", 'i'); map("ÌÍÎÏĨĪĬĮİ", 'I')
    map("ñńņň", 'n'); map("ÑŃŅŇ", 'N')
    map("òóôõöøōŏő", 'o'); map("ÒÓÔÕÖØŌŎŐ", 'O')
    map("śŝşšș", 's'); map("ŚŜŞŠȘ", 'S')
    map("ţťț", 't'); map("ŢŤȚ", 'T')
    map("ùúûüũūŭůűų", 'u'); map("ÙÚÛÜŨŪŬŮŰŲ", 'U')
    map("ýÿ", 'y'); map("ÝŸ", 'Y')
    map("źżž", 'z'); map("ŹŻŽ", 'Z')
}

internal fun subtitleOf(vararg parts: Any?): String? =
    parts.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
        .joinToString(" • ")
        .takeIf { it.isNotEmpty() }
