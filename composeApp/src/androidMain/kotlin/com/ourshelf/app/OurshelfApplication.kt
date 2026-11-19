package com.ourshelf.app

import android.app.Application
import com.ourshelf.app.core.Platform
import com.ourshelf.app.di.ourshelfModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OurshelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Platform.init(this)
        startKoin {
            androidContext(this@OurshelfApplication)
            modules(ourshelfModules())
        }
    }
}
