@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ourshelf.app.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun IsbnCameraView(
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    var authorized by remember { mutableStateOf<Boolean?>(null) }
    val currentOnDenied by rememberUpdatedState(onPermissionDenied)
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> authorized = true
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        authorized = granted
                        if (!granted) currentOnDenied()
                    }
                }
            else -> {
                authorized = false
                currentOnDenied()
            }
        }
    }
    if (authorized != true) return

    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val scanner = remember { BarcodeCaptureController { currentOnBarcode(it) } }
    DisposableEffect(scanner) {
        scanner.start()
        onDispose { scanner.stop() }
    }
    UIKitView(factory = { scanner.view }, modifier = modifier)
}

/** Önizleme katmanını görünüm boyutuna uyduran basit kapsayıcı. */
private class PreviewContainerView(private val previewLayer: AVCaptureVideoPreviewLayer) :
    UIView(frame = CGRectZero.readValue()) {
    init {
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.setFrame(bounds)
    }
}

/** AVCaptureSession + EAN-13 meta veri çıktısı. Simülatörde kamera olmadığı için sessizce hiçbir şey yapmaz. */
private class BarcodeCaptureController(private val onCode: (String) -> Unit) :
    NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val session = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session).apply {
        videoGravity = AVLayerVideoGravityResizeAspectFill
    }
    val view: UIView = PreviewContainerView(previewLayer)

    init {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) as? AVCaptureDeviceInput }
        if (input != null && session.canAddInput(input)) {
            session.addInput(input)
            val output = AVCaptureMetadataOutput()
            if (session.canAddOutput(output)) {
                session.addOutput(output)
                output.setMetadataObjectsDelegate(this, queue = dispatch_get_main_queue())
                output.metadataObjectTypes = listOf(AVMetadataObjectTypeEAN13Code)
            }
        }
    }

    fun start() {
        // startRunning engelleyici bir çağrıdır; ana iş parçacığında yapılmamalı.
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            if (!session.running) session.startRunning()
        }
    }

    fun stop() {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            if (session.running) session.stopRunning()
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        (didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.stringValue?.let(onCode)
    }
}
