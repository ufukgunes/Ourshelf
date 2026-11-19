package com.ourshelf.app

import androidx.compose.ui.window.ComposeUIViewController
import com.ourshelf.app.di.ourshelfModules
import com.ourshelf.app.ui.navigation.OurshelfApp
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/** Swift tarafından çağrılan giriş noktası (`MainViewControllerKt.MainViewController()`). */
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        startKoin { modules(ourshelfModules()) }
        koinStarted = true
    }
    return ComposeUIViewController { OurshelfApp() }
}
