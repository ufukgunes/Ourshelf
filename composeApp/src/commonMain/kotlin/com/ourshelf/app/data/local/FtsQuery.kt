package com.ourshelf.app.data.local

/**
 * Kullanıcı girdisini güvenli bir FTS4 MATCH ifadesine çevirir.
 * Girdi yalnızca harf/rakam kelimelerine bölünür (FTS operatörleri ve tırnaklar atılır); her kelime
 * tırnak içinde önek araması olarak yazılır (FTS4 sözdizimi: yıldız tırnağın İÇİNDE):
 * `incep nol` -> `"incep*" "nol*"`
 * Kullanılabilir kelime yoksa null döner.
 */
object FtsQuery {
    private val tokenSplitter = Regex("[^\\p{L}\\p{N}]+")

    fun build(raw: String): String? {
        val tokens = raw
            .split(tokenSplitter)
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it*\"" }
    }
}
