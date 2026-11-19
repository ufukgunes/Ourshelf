package com.ourshelf.app.data.share

import com.ourshelf.app.core.Isbn
import com.ourshelf.app.core.model.MediaType

/** Başka bir uygulamadan paylaşılan metnin neyi işaret ettiği. */
sealed interface SharedTarget {
    data class Imdb(val id: String) : SharedTarget
    data class Tmdb(val type: MediaType, val id: Long) : SharedTarget
    data class Book(val isbn: String) : SharedTarget
    data class Text(val query: String) : SharedTarget
}

/**
 * Paylaşılan metni tanır: IMDb/TMDB bağlantısı, ISBN ya da serbest başlık.
 * Serbest metinde bağlantılar ve "Official Trailer", "Fragman" gibi ekler temizlenir
 * (örn. YouTube: "Dune: Part Two | Official Trailer - YouTube https://youtu.be/…" → "Dune: Part Two").
 * Not: Yalnızca temel regex sınıfları kullanılır (iOS/Kotlin Native uyumu).
 */
object SharedTextParser {

    private val imdb = Regex("imdb\\.com/(?:[a-z]{2}/)?title/(tt[0-9]{5,10})", RegexOption.IGNORE_CASE)
    private val tmdb = Regex("themoviedb\\.org/(movie|tv)/([0-9]+)", RegexOption.IGNORE_CASE)
    private val url = Regex("https?://[^ \\n\\t]+")
    private val separators = listOf(" | ", " - ", " – ", " — ", " · ")
    private val noise = Regex(
        "\\b(official|resmi|final|teaser|trailer|fragman|tanıtım|hd|4k|youtube|altyazılı|türkçe|dublaj)\\b",
        RegexOption.IGNORE_CASE,
    )

    fun parse(raw: String): SharedTarget? {
        imdb.find(raw)?.let { return SharedTarget.Imdb(it.groupValues[1].lowercase()) }
        tmdb.find(raw)?.let { match ->
            val type = if (match.groupValues[1].equals("tv", ignoreCase = true)) MediaType.TV_SHOW else MediaType.MOVIE
            return SharedTarget.Tmdb(type, match.groupValues[2].toLong())
        }
        raw.split(' ', '\n', '\t', ',', ';').firstOrNull { Isbn.looksLikeIsbn(it) }?.let {
            return SharedTarget.Book(Isbn.normalize(it))
        }
        return cleanTitle(raw)?.let(SharedTarget::Text)
    }

    internal fun cleanTitle(raw: String): String? {
        val firstLine = raw.replace(url, " ").lines().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        var title = firstLine
        for (sep in separators) {
            val idx = title.indexOf(sep)
            if (idx > 0) title = title.substring(0, idx)
        }
        title = title.replace(noise, " ")
            .replace("()", " ").replace("[]", " ")
            .trim(' ', '"', '\'', '“', '”', '«', '»', '-', ':', '|')
            .replace(Regex(" {2,}"), " ")
        return title.takeIf { it.length >= 2 }?.take(80)
    }
}
