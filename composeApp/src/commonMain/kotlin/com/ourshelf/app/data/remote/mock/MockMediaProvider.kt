package com.ourshelf.app.data.remote.mock

import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider
import com.ourshelf.app.data.remote.foldDiacritics
import kotlinx.coroutines.delay

/**
 * API anahtarı olmadan geliştirme ve UI testleri için örnek veri sağlayıcısı.
 * Poster URL'si yoktur; arayüz başlıktan üretilen gradient posteri gösterir.
 */
class MockMediaProvider(
    override val supportedTypes: Set<MediaType> = MediaType.entries.toSet(),
    private val latencyMs: Long = 250,
    private val catalog: List<MediaSearchResult> = MockCatalog.items,
) : MediaSearchProvider {

    override val name = "Mock"

    override suspend fun search(query: String, type: MediaType, page: Int): Result<List<MediaSearchResult>> {
        if (latencyMs > 0) delay(latencyMs)
        if (page > 1 || type !in supportedTypes) return Result.success(emptyList())
        val needle = query.normalized()
        if (needle.isBlank()) return Result.success(emptyList())
        val results = catalog.filter { item ->
            item.mediaType == type && (
                item.title.normalized().contains(needle) ||
                    item.subtitle.orEmpty().normalized().contains(needle) ||
                    item.genres.any { it.normalized().contains(needle) }
                )
        }
        return Result.success(results)
    }

    override suspend fun details(result: MediaSearchResult): Result<MediaSearchResult> =
        Result.success(catalog.firstOrNull { it.externalId == result.externalId } ?: result)

    private fun String.normalized(): String = foldDiacritics(lowercase()).trim()
}

internal object MockCatalog {
    private fun movie(
        n: Int, title: String, director: String, year: Int, minutes: Int,
        genres: List<String>, overview: String, providers: List<String> = emptyList(),
    ) = MediaSearchResult(
        externalId = "mock:movie:$n", mediaType = MediaType.MOVIE, title = title,
        subtitle = "$director • $year", releaseYear = year, lengthValue = minutes,
        genres = genres, overview = overview,
        metadata = MediaMetadata(creators = listOf(director), streamingProviders = providers),
    )

    private fun tv(
        n: Int, title: String, creator: String, year: Int, episodes: Int, seasons: Int,
        genres: List<String>, overview: String,
    ) = MediaSearchResult(
        externalId = "mock:tv:$n", mediaType = MediaType.TV_SHOW, title = title,
        subtitle = "$creator • $year", releaseYear = year, lengthValue = episodes,
        genres = genres, overview = overview,
        metadata = MediaMetadata(creators = listOf(creator), seasonCount = seasons),
    )

    private fun book(
        n: Int, title: String, author: String, year: Int, pages: Int,
        genres: List<String>, overview: String,
    ) = MediaSearchResult(
        externalId = "mock:book:$n", mediaType = MediaType.BOOK, title = title,
        subtitle = "$author • $year", releaseYear = year, lengthValue = pages,
        genres = genres, overview = overview,
        metadata = MediaMetadata(creators = listOf(author)),
    )

    private fun game(
        n: Int, title: String, studio: String, year: Int, hours: Int,
        platforms: List<String>, genres: List<String>, overview: String,
    ) = MediaSearchResult(
        externalId = "mock:game:$n", mediaType = MediaType.GAME, title = title,
        subtitle = "$studio • $year", releaseYear = year, lengthValue = hours,
        genres = genres, overview = overview,
        metadata = MediaMetadata(creators = listOf(studio), platforms = platforms),
    )

    private fun audio(
        n: Int, type: MediaType, title: String, artist: String, year: Int, tracks: Int,
        genres: List<String>,
    ) = MediaSearchResult(
        externalId = "mock:${type.name.lowercase()}:$n", mediaType = type, title = title,
        subtitle = "$artist • $year", releaseYear = year, lengthValue = tracks,
        genres = genres, metadata = MediaMetadata(creators = listOf(artist)),
    )

    val items: List<MediaSearchResult> = listOf(
        movie(1, "Inception", "Christopher Nolan", 2010, 148, listOf("Bilim Kurgu", "Gerilim"),
            "Rüyalara sızarak sır çalan bir hırsız, bu kez bir fikri birinin zihnine yerleştirmek zorundadır.",
            listOf("Netflix")),
        movie(2, "Interstellar", "Christopher Nolan", 2014, 169, listOf("Bilim Kurgu", "Dram"),
            "Kuruyan bir dünyada bir grup kaşif, insanlık için yeni bir yuva bulmak üzere bir solucan deliğinden geçer."),
        movie(3, "Spirited Away", "Hayao Miyazaki", 2001, 125, listOf("Animasyon", "Fantastik"),
            "Ailesiyle birlikte gizemli bir ruhlar dünyasına sürüklenen küçük bir kız, eve dönmenin yolunu arar.",
            listOf("Netflix")),
        movie(4, "Parasite", "Bong Joon-ho", 2019, 132, listOf("Dram", "Gerilim"),
            "Yoksul bir aile, varlıklı bir ailenin hayatına adım adım sızar; sonuçları kimsenin beklediği gibi olmaz."),
        movie(5, "The Grand Budapest Hotel", "Wes Anderson", 2014, 99, listOf("Komedi", "Macera"),
            "Efsanevi bir otel konsiyerji ve çırağı, değerli bir tablo yüzünden kendilerini bir maceranın ortasında bulur.",
            listOf("Disney+")),
        movie(6, "Kış Uykusu", "Nuri Bilge Ceylan", 2014, 196, listOf("Dram"),
            "Kapadokya'da bir otel işleten emekli bir oyuncunun kış boyunca yaşadığı iç hesaplaşma."),
        movie(7, "Arrival", "Denis Villeneuve", 2016, 116, listOf("Bilim Kurgu", "Dram"),
            "Bir dilbilimci, Dünya'ya gelen yabancılarla iletişim kurmanın yolunu bulmaya çalışır.",
            listOf("Prime Video")),
        movie(8, "Before Sunrise", "Richard Linklater", 1995, 101, listOf("Romantik", "Dram"),
            "Trende tanışan iki yabancı, Viyana'da tek bir geceyi birlikte geçirmeye karar verir."),
        movie(9, "Mad Max: Fury Road", "George Miller", 2015, 120, listOf("Aksiyon", "Macera"),
            "Kıyamet sonrası bir çölde, kaçış hiç durmayan bir takibe dönüşür."),
        movie(10, "Amélie", "Jean-Pierre Jeunet", 2001, 122, listOf("Komedi", "Romantik"),
            "Paris'te utangaç bir garson kız, çevresindeki insanların hayatlarını gizlice güzelleştirmeye başlar."),
        movie(11, "Dune: Part Two", "Denis Villeneuve", 2024, 166, listOf("Bilim Kurgu", "Macera"),
            "Paul Atreides, ailesinin intikamını almak için çöl halkıyla güçlerini birleştirir.",
            listOf("HBO Max")),
        movie(12, "Coco", "Lee Unkrich", 2017, 105, listOf("Animasyon", "Aile"),
            "Müzisyen olmayı hayal eden bir çocuk, Ölüler Diyarı'nda ailesinin geçmişini keşfeder.",
            listOf("Disney+")),

        tv(1, "Breaking Bad", "Vince Gilligan", 2008, 62, 5, listOf("Suç", "Dram"),
            "Kanser teşhisi konan bir kimya öğretmeni, ailesinin geleceği için tehlikeli bir yola girer."),
        tv(2, "Dark", "Baran bo Odar", 2017, 26, 3, listOf("Bilim Kurgu", "Gizem"),
            "Küçük bir Alman kasabasında kaybolan çocuklar, dört aileyi zamanın içinde birbirine bağlar."),
        tv(3, "Severance", "Dan Erickson", 2022, 19, 2, listOf("Gerilim", "Bilim Kurgu"),
            "Çalışanların iş ve özel hayat anıları cerrahi olarak ayrılan gizemli bir şirkette tuhaf şeyler olur."),
        tv(4, "The Bear", "Christopher Storer", 2022, 38, 4, listOf("Dram", "Komedi"),
            "Ünlü bir şef, ailesinin Chicago'daki dağınık sandviç dükkanını devralır."),
        tv(5, "Shōgun", "Rachel Kondo", 2024, 10, 1, listOf("Tarih", "Dram"),
            "1600'lerin Japonya'sında bir İngiliz denizci, güç savaşlarının tam ortasına düşer."),
        tv(6, "Bir Başkadır", "Berkun Oya", 2020, 8, 1, listOf("Dram"),
            "İstanbul'da birbirinden çok farklı hayatlar, beklenmedik biçimlerde kesişir.",
        ),

        book(1, "Dune", "Frank Herbert", 1965, 612, listOf("Bilim Kurgu"),
            "Çöl gezegeni Arrakis'te geçen; siyaset, din ve ekolojiyi harmanlayan klasik bir destan."),
        book(2, "Kürk Mantolu Madonna", "Sabahattin Ali", 1943, 160, listOf("Roman", "Klasik"),
            "Silik bir memurun, Berlin'de tanıştığı bir ressama duyduğu sessiz ve derin aşkın hikâyesi."),
        book(3, "Project Hail Mary", "Andy Weir", 2021, 496, listOf("Bilim Kurgu"),
            "Hafızasını kaybetmiş bir bilim insanı, uzak bir yıldız sisteminde insanlığı kurtarmaya çalışır."),
        book(4, "Saatleri Ayarlama Enstitüsü", "Ahmet Hamdi Tanpınar", 1961, 382, listOf("Roman", "Hiciv"),
            "Absürt bir kurumun hikâyesi üzerinden modernleşme sancılarına ironik bir bakış."),
        book(5, "The Name of the Wind", "Patrick Rothfuss", 2007, 662, listOf("Fantastik"),
            "Efsanevi bir büyücü, hanında kendi hayat hikâyesini ilk kez anlatmaya başlar."),
        book(6, "Atomic Habits", "James Clear", 2018, 320, listOf("Kişisel Gelişim"),
            "Küçük alışkanlıkların zamanla nasıl büyük değişimlere dönüştüğüne dair pratik bir rehber."),

        game(1, "The Legend of Zelda: Tears of the Kingdom", "Nintendo", 2023, 60,
            listOf("Switch"), listOf("Macera", "Açık Dünya"),
            "Gökyüzü adalarından yeraltına uzanan dev bir krallıkta yaratıcılığa dayalı bir macera."),
        game(2, "Elden Ring", "FromSoftware", 2022, 58,
            listOf("PS5", "Xbox Series", "PC"), listOf("RPG", "Aksiyon"),
            "Gizemlerle dolu, zorlu ve büyüleyici bir açık dünya rol yapma oyunu."),
        game(3, "Hades", "Supergiant Games", 2020, 22,
            listOf("PC", "Switch", "PS5"), listOf("Roguelike", "Aksiyon"),
            "Yeraltı dünyasının prensi, babasının krallığından kaçmak için tekrar tekrar savaşır."),
        game(4, "Stardew Valley", "ConcernedApe", 2016, 53,
            listOf("PC", "Switch", "Mobil"), listOf("Simülasyon", "Rahatlatıcı"),
            "Dededen kalan çiftliği canlandırırken bir kasaba topluluğunun parçası olursunuz."),
        game(5, "Baldur's Gate 3", "Larian Studios", 2023, 75,
            listOf("PC", "PS5"), listOf("RPG", "Strateji"),
            "Seçimlerin gerçekten önemli olduğu, derin ve sıra tabanlı bir fantastik rol yapma oyunu."),
        game(6, "Outer Wilds", "Mobius Digital", 2019, 17,
            listOf("PC", "PS5", "Switch"), listOf("Keşif", "Gizem"),
            "22 dakikada bir tekrarlanan bir güneş sisteminde, kadim bir gizemi çözmeye çalışırsınız."),

        audio(1, MediaType.ALBUM, "Random Access Memories", "Daft Punk", 2013, 13, listOf("Elektronik")),
        audio(2, MediaType.ALBUM, "Kind of Blue", "Miles Davis", 1959, 5, listOf("Caz")),
        audio(3, MediaType.ALBUM, "Anadolu Pop Klasikleri", "Çeşitli Sanatçılar", 1975, 14, listOf("Anadolu Rock")),
        audio(1, MediaType.PODCAST, "Hardcore History", "Dan Carlin", 2006, 70, listOf("Tarih")),
        audio(2, MediaType.PODCAST, "Darknet Diaries", "Jack Rhysider", 2017, 150, listOf("Teknoloji")),
        audio(3, MediaType.PODCAST, "Bilim Kazanı", "Bilim Kazanı", 2019, 120, listOf("Bilim")),
    )
}
