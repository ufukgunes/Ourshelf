package com.ourshelf.app.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImageSharer(): ImageSharer = remember {
    ImageSharer { image, _ ->
        val png = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: return@ImageSharer
        val data = png.usePinned { NSData.dataWithBytes(it.addressOf(0), png.size.toULong()) }
        val uiImage = UIImage.imageWithData(data) ?: return@ImageSharer
        val controller = UIActivityViewController(activityItems = listOf(uiImage), applicationActivities = null)
        val presenter = topViewController() ?: return@ImageSharer
        // iPad'de paylaşım menüsü bir kaynak görünüme bağlanmalıdır.
        controller.popoverPresentationController?.sourceView = presenter.view
        presenter.presentViewController(controller, animated = true, completion = null)
    }
}

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
