package com.ourshelf.app.core

import android.content.Context
import android.util.Log
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant

actual object Platform {
    private lateinit var appContext: Context

    /** Application.onCreate içinde çağrılır. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual val languageTag: String get() = Locale.getDefault().toLanguageTag()

    actual val region: String get() = Locale.getDefault().country

    actual val filesDir: String get() = appContext.filesDir.absolutePath

    actual fun formatDate(instant: Instant): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(instant.toJavaInstant())

    actual fun logWarning(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }
}
