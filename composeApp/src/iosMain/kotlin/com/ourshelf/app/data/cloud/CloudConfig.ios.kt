package com.ourshelf.app.data.cloud

import com.ourshelf.app.Secrets

internal actual fun platformApiKey(): String = Secrets.FIREBASE_API_KEY_IOS
