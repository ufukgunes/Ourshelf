package com.ourshelf.app.core

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSLog
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.preferredLanguages
import kotlin.time.Instant

actual object Platform {
    actual val languageTag: String
        get() = (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en-US"

    actual val region: String
        get() = NSLocale.currentLocale.countryCode ?: ""

    /** Application Support: kullanıcıya görünmez, iCloud yedeğine dahil. */
    actual val filesDir: String by lazy {
        val dir = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first() as String
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        dir
    }

    actual fun formatDate(instant: Instant): String {
        val formatter = NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterNoStyle
        }
        val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
        return formatter.stringFromDate(date)
    }

    actual fun logWarning(tag: String, message: String, throwable: Throwable?) {
        NSLog("W/$tag: $message ${throwable?.message.orEmpty()}")
    }
}
