package com.ourshelf.app.data.image

import android.graphics.Bitmap
import android.os.Build
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.ByteArrayOutputStream

internal actual fun ImageRequest.Builder.allowPixelAccess(): ImageRequest.Builder = allowHardware(false)

internal actual fun Image.samplePixels(grid: Int): IntArray? {
    val bitmap = runCatching { toBitmap() }.getOrNull() ?: return null
    if (bitmap.width == 0 || bitmap.height == 0) return null
    val cols = minOf(grid, bitmap.width)
    val rows = minOf(grid, bitmap.height)
    return IntArray(cols * rows) { i ->
        val x = (i % cols) * bitmap.width / cols
        val y = (i / cols) * bitmap.height / rows
        bitmap.getPixel(x, y)
    }
}

internal actual fun Image.encodeWebp(quality: Int): ByteArray? {
    val bitmap = runCatching { toBitmap() }.getOrNull() ?: return null
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
    return ByteArrayOutputStream().use { out ->
        if (bitmap.compress(format, quality, out)) out.toByteArray() else null
    }
}
