package com.ourshelf.app.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ourshelf.app.core.Isbn
import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.action_back
import com.ourshelf.app.resources.scan_hint
import com.ourshelf.app.resources.scan_not_a_book
import com.ourshelf.app.resources.scan_permission_denied
import com.ourshelf.app.resources.scan_title
import com.ourshelf.app.ui.motion.rememberHaptics
import org.jetbrains.compose.resources.stringResource

/**
 * Kamera önizlemesi; okunan her barkodu [onBarcode] ile bildirir. İzin isteme platform tarafında yapılır,
 * reddedilirse [onPermissionDenied] çağrılır. (Android: CameraX + ML Kit, iOS: AVFoundation.)
 */
@Composable
expect fun IsbnCameraView(
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
)

/** Tam ekran ISBN tarayıcı. Geçerli bir kitap barkodu okununca bir kez [onIsbn] çağrılır. */
@Composable
fun IsbnScanScreen(onIsbn: (String) -> Unit, onClose: () -> Unit) {
    val haptics = rememberHaptics()
    var done by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var lastInvalid by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!denied) {
            IsbnCameraView(
                onBarcode = { raw ->
                    if (done) return@IsbnCameraView
                    val isbn = Isbn.fromBarcode(raw)
                    if (isbn != null) {
                        done = true
                        haptics.confirm()
                        onIsbn(isbn)
                    } else {
                        lastInvalid = raw
                    }
                },
                onPermissionDenied = { denied = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_back), tint = Color.White)
                }
                Text(
                    stringResource(Res.string.scan_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(Modifier.weight(1f))
            if (denied) {
                Text(
                    stringResource(Res.string.scan_permission_denied),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            } else {
                // Barkod çerçevesi (EAN-13 yatay ve basık olduğu için geniş bir dikdörtgen).
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(2.2f)
                        .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(if (lastInvalid != null) Res.string.scan_not_a_book else Res.string.scan_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.weight(1.3f))
        }
    }
}
