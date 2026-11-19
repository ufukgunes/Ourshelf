package com.ourshelf.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.ourshelf.app.ui.intake.ShareIntakeSheet
import com.ourshelf.app.ui.theme.OurshelfTheme
import org.koin.compose.koinInject

/**
 * "Paylaş → Ourshelf" hedefi. Saydam bir pencerede yalnızca alt kartı gösterir;
 * kullanıcı paylaştığı uygulamadan ayrılmış gibi hissetmez.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val text = sharedText(intent)
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            val imageLoader = koinInject<ImageLoader>()
            setSingletonImageLoaderFactory { imageLoader }
            OurshelfTheme {
                ShareIntakeSheet(sharedText = text, onClose = ::finish)
            }
        }
    }

    private fun sharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        // Bazı uygulamalar başlığı konu alanında, bağlantıyı metinde gönderir.
        return listOf(subject, text).filter { it.isNotBlank() }.joinToString("\n").ifBlank { null }
    }
}
