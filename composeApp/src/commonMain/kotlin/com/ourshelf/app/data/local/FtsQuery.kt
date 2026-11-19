package com.ourshelf.app.data.local

/**
 * Kullanıcı girdisini güvenli bir FTS4 MATCH ifadesine çevirir.
 * Girdi yalnızca harf/rakam kelimelerine bölünür (FTS operatörleri ve tırnaklar atılır); her kelime
 * tırnak içinde önek araması olarak yazılır (FTS4 sözdizimi: yıldız tırnağın İÇİNDE):
 * `incep nol` -> `"incep*" "nol*"`
 * Kullanılabilir kelime yoksa null döner.
 *
 * Not: Bölme işlemi regex yerine `Char.isLetterOrDigit()` ile yapılır; `\p{L}` gibi Unicode sınıfları
 * Kotlin/Native (iOS) regex motorunda desteklenmez.
 */
object FtsQuery {
    fun build(raw: String): String? {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (c in raw) {
            if (c.isLetterOrDigit()) {
                current.append(c)
            } else if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "\"$it*\"" }
    }
}
