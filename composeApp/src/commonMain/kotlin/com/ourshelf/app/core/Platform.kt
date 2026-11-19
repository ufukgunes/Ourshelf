package com.ourshelf.app.core

import kotlin.time.Instant

/** Platforma özgü küçük yardımcılar. */
expect object Platform {
    /** BCP-47 dil etiketi, örn. "tr-TR". */
    val languageTag: String

    /** ISO ülke kodu, örn. "TR". Bilinmiyorsa boş. */
    val region: String

    /** Uygulamanın kalıcı dosya dizini (posterler vb.). */
    val filesDir: String

    /** Cihazın yerel ayarlarına göre orta uzunlukta tarih, örn. "18 Kas 2026". */
    fun formatDate(instant: Instant): String

    fun logWarning(tag: String, message: String, throwable: Throwable? = null)
}
