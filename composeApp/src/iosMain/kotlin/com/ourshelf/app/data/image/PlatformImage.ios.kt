package com.ourshelf.app.data.image

import coil3.Image
import coil3.request.ImageRequest
import coil3.toBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage

internal actual fun ImageRequest.Builder.allowPixelAccess(): ImageRequest.Builder = this

internal actual fun Image.samplePixels(grid: Int): IntArray? {
    val bitmap = runCatching { toBitmap() }.getOrNull() ?: return null
    if (bitmap.width == 0 || bitmap.height == 0) return null
    val cols = minOf(grid, bitmap.width)
    val rows = minOf(grid, bitmap.height)
    return IntArray(cols * rows) { i ->
        val x = (i % cols) * bitmap.width / cols
        val y = (i / cols) * bitmap.height / rows
        bitmap.getColor(x, y)
    }
}

internal actual fun Image.encodeWebp(quality: Int): ByteArray? {
    val bitmap = runCatching { toBitmap() }.getOrNull() ?: return null
    return SkiaImage.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.WEBP, quality)?.bytes
}
