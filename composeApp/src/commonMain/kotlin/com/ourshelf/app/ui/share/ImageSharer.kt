package com.ourshelf.app.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Bir görseli sistemin paylaşım menüsüyle paylaşır (Android: Sharesheet, iOS: UIActivityViewController). */
fun interface ImageSharer {
    fun share(image: ImageBitmap, fileName: String)
}

@Composable
expect fun rememberImageSharer(): ImageSharer
