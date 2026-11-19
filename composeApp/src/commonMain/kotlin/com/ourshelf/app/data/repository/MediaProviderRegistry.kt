package com.ourshelf.app.data.repository

import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.data.remote.MediaSearchProvider

/** Her medya türü için hangi sağlayıcının kullanılacağını belirler. */
class MediaProviderRegistry(
    private val providers: Map<MediaType, MediaSearchProvider>,
) {
    init {
        val missing = MediaType.entries.filter { it !in providers }
        require(missing.isEmpty()) { "Sağlayıcısı olmayan türler: $missing" }
    }

    fun providerFor(type: MediaType): MediaSearchProvider = providers.getValue(type)

    /** Gerçek veri yerine örnek veriyle çalışan türler (arayüzde "demo" ipucu göstermek için). */
    val mockTypes: Set<MediaType>
        get() = providers.filterValues { it.name == "Mock" }.keys
}
