package com.ourshelf.app.data.image

import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.ourshelf.app.core.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

data class ProcessedPoster(
    val fileName: String,
    val dominantColor: Int?,
    val accentColor: Int?,
)

/**
 * Kaydedilen eserin posterini indirir, cihaza WebP olarak yazar ve ambiyans renklerini çıkarır.
 * Renkler bir kez hesaplanıp DB'ye yazılır; arka plan her açılışta yeniden hesaplamaz.
 */
interface PosterProcessor {
    suspend fun process(externalId: String, url: String): ProcessedPoster?
}

/** Posterlerin saklandığı dizin (Auto Backup'tan hariç tutulur). */
fun posterDirectory(): Path = Platform.filesDir.toPath() / POSTER_DIR

fun posterPath(fileName: String): Path = posterDirectory() / fileName

const val POSTER_DIR = "posters"

fun posterFileNameFor(externalId: String) = externalId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".webp"

class CoilPosterProcessor(
    private val context: PlatformContext,
    private val imageLoader: ImageLoader,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : PosterProcessor {

    override suspend fun process(externalId: String, url: String): ProcessedPoster? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowPixelAccess()
            .build()
        val image = (imageLoader.execute(request) as? SuccessResult)?.image ?: return null

        return withContext(Dispatchers.Default) {
            val colors = image.samplePixels(SAMPLE_GRID)?.let(ColorExtractor::extract)
                ?: ColorExtractor.Colors(null, null)
            val bytes = image.encodeWebp(quality = 85) ?: return@withContext null

            val fileName = posterFileNameFor(externalId)
            withContext(Dispatchers.IO) {
                val dir = posterDirectory()
                fileSystem.createDirectories(dir)
                val tmp = dir / "$fileName.tmp"
                fileSystem.write(tmp) { write(bytes) }
                fileSystem.atomicMove(tmp, dir / fileName)
            }
            ProcessedPoster(fileName, colors.dominant, colors.accent)
        }
    }

    private companion object {
        const val SAMPLE_GRID = 64
    }
}

/** Palette/compress için yazılım bitmap'i gerekir (Android'de donanım bitmap'i kapatılır). */
internal expect fun ImageRequest.Builder.allowPixelAccess(): ImageRequest.Builder

/** Görselden en fazla [grid]×[grid] eşit aralıklı ARGB piksel örneği alır. */
internal expect fun Image.samplePixels(grid: Int): IntArray?

internal expect fun Image.encodeWebp(quality: Int): ByteArray?
