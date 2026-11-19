package com.ourshelf.app.data.remote.openlibrary

import com.ourshelf.app.core.Isbn
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.foldDiacritics
import com.ourshelf.app.data.remote.subtitleOf
import com.ourshelf.app.data.remote.suspendRunCatching
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class OpenLibraryApi(private val client: HttpClient) {
    suspend fun search(query: String, page: Int, limit: Int = 20): OpenLibrarySearchResponse =
        client.get("search.json") {
            parameter("q", query); parameter("page", page); parameter("limit", limit); parameter("fields", FIELDS)
        }.body()

    suspend fun searchByIsbn(isbn: String): OpenLibrarySearchResponse =
        client.get("search.json") { parameter("isbn", isbn); parameter("fields", FIELDS) }.body()

    suspend fun work(workId: String): OpenLibraryWork = client.get("works/$workId.json").body()

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        const val FIELDS = "key,title,author_name,first_publish_year,cover_i,number_of_pages_median,isbn,subject"
    }
}

@Serializable
data class OpenLibrarySearchResponse(
    val numFound: Int = 0,
    val docs: List<OpenLibraryDoc> = emptyList(),
)

@Serializable
data class OpenLibraryDoc(
    val key: String,
    val title: String = "",
    @SerialName("author_name") val authorName: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("number_of_pages_median") val pages: Int? = null,
    val isbn: List<String> = emptyList(),
    val subject: List<String> = emptyList(),
)

@Serializable
data class OpenLibraryWork(
    /** Bazen düz metin, bazen `{ "type": ..., "value": "..." }` nesnesi. */
    val description: JsonElement? = null,
    val subjects: List<String> = emptyList(),
)

class OpenLibraryProvider(private val api: OpenLibraryApi) : MediaSearchProvider {
    override val name = "OpenLibrary"
    override val supportedTypes = setOf(MediaType.BOOK)

    override suspend fun search(query: String, type: MediaType, page: Int) = suspendRunCatching {
        if (type != MediaType.BOOK) return@suspendRunCatching emptyList()
        // Arama kutusuna yazılan ya da barkoddan okunan ISBN doğrudan ISBN uç noktasıyla aranır.
        val docs = if (Isbn.looksLikeIsbn(query)) {
            if (page > 1) emptyList() else api.searchByIsbn(Isbn.normalize(query)).docs
        } else {
            api.search(query, page).docs
        }
        dedupeWorks(docs.map { it.toResult() })
    }

    suspend fun searchByIsbn(isbn: String) = suspendRunCatching {
        api.searchByIsbn(isbn).docs.map { it.toResult() }
    }

    override suspend fun details(result: MediaSearchResult) = suspendRunCatching {
        val workId = result.externalId.removePrefix(EXTERNAL_PREFIX)
        val work = api.work(workId)
        result.copy(
            overview = work.descriptionText() ?: result.overview,
            genres = result.genres.ifEmpty { work.subjects.cleanSubjects() },
        )
    }

    companion object {
        const val EXTERNAL_PREFIX = "ol:"
        fun coverUrl(coverId: Long?) = coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
    }
}

/**
 * OpenLibrary aynı kitabı birden fazla "work" kaydı olarak döndürebilir ("Kürk Mantolu Madonna" ×5).
 * Başlık + ilk yazar aynı olanlar birleştirilir; kapağı olan kayıt tercih edilir, ilk görünme sırası korunur.
 */
internal fun dedupeWorks(results: List<MediaSearchResult>): List<MediaSearchResult> =
    results
        .groupBy { normalizedKey(it.title) + "|" + normalizedKey(it.metadata.creators.firstOrNull().orEmpty()) }
        .values
        .map { group -> group.firstOrNull { it.posterUrl != null } ?: group.first() }

/** "Science-fiction", "Science fiction" ve "Kürk" / "Kurk" aynı anahtara iner. */
internal fun normalizedKey(text: String): String =
    foldDiacritics(text.lowercase()).filter { it.isLetterOrDigit() }

internal fun OpenLibraryDoc.toResult(): MediaSearchResult {
    val workId = key.substringAfterLast('/')
    return MediaSearchResult(
        externalId = OpenLibraryProvider.EXTERNAL_PREFIX + workId,
        mediaType = MediaType.BOOK,
        title = title,
        subtitle = subtitleOf(authorName.firstOrNull(), firstPublishYear),
        posterUrl = OpenLibraryProvider.coverUrl(coverId),
        releaseYear = firstPublishYear,
        lengthValue = pages?.takeIf { it > 0 },
        genres = subject.cleanSubjects(),
        metadata = MediaMetadata(
            creators = authorName,
            isbn = isbn.firstOrNull { it.length == 13 } ?: isbn.firstOrNull(),
            sourceUrl = "https://openlibrary.org$key",
        ),
    )
}

internal fun OpenLibraryWork.descriptionText(): String? = when (val d = description) {
    is JsonPrimitive -> d.contentOrNull
    is JsonObject -> d["value"]?.jsonPrimitive?.contentOrNull
    else -> null
}?.trim()?.takeIf { it.isNotEmpty() }

/** "Fiction, general", "nyt:bestseller", "Dune (Imaginary place)" gibi gürültülü konuları eler. */
private const val NOISY_SUBJECT_CHARS = ":=,()"

/** Tür değil, kütüphane/katalog etiketi olan konular. */
private val NOISY_SUBJECTS = Regex(
    "reviewed|accessible book|protected daisy|in library|large type|staff picks|bestseller|lending library|overdrive",
    RegexOption.IGNORE_CASE,
)

/** OpenLibrary konuları çok gürültülü; kısa ve okunabilir ilk 3 tanesini al. */
private fun List<String>.cleanSubjects(): List<String> =
    asSequence()
        .filter { s -> s.length in 3..24 && s.none { it in NOISY_SUBJECT_CHARS } && !NOISY_SUBJECTS.containsMatchIn(s) }
        .map { s -> s.replaceFirstChar { it.uppercase() } }
        .distinctBy(::normalizedKey)
        .take(3)
        .toList()
