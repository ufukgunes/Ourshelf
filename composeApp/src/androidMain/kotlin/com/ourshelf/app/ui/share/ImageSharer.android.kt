package com.ourshelf.app.ui.share

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * PNG'yi önbellekteki `shared/` klasörüne yazar ve FileProvider URI'siyle paylaşır.
 * Eski paylaşım dosyaları her seferinde temizlenir.
 */
@Composable
actual fun rememberImageSharer(): ImageSharer {
    val context = LocalContext.current
    return remember(context) {
        ImageSharer { image, fileName ->
            val dir = File(context.cacheDir, "shared").apply {
                mkdirs()
                listFiles()?.forEach { it.delete() }
            }
            val file = File(dir, fileName)
            file.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(fileName, uri) // Sharesheet önizlemesi için
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
