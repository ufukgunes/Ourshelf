package com.ourshelf.app.core

/**
 * ISBN yardımcıları. Kitapların arka kapağındaki barkod EAN-13 biçimindedir ve ISBN'ler 978/979 ile başlar.
 */
object Isbn {

    /** Barkoddan gelen değeri doğrular; geçerli bir ISBN-13 ise rakamlarını, değilse null döndürür. */
    fun fromBarcode(raw: String): String? {
        val digits = raw.filter(Char::isDigit)
        if (digits.length != 13 || !(digits.startsWith("978") || digits.startsWith("979"))) return null
        return digits.takeIf(::isValidIsbn13)
    }

    /** Kullanıcının yazdığı metin bir ISBN mi? (Arama kutusuna ISBN yazılabilir; tire ve boşluk serbest.) */
    fun looksLikeIsbn(query: String): Boolean {
        val compact = query.filterNot { it == '-' || it == ' ' }
        return (compact.length == 13 && compact.all(Char::isDigit) && isValidIsbn13(compact)) ||
            (compact.length == 10 && compact.dropLast(1).all(Char::isDigit) && isValidIsbn10(compact))
    }

    fun normalize(query: String): String = query.filterNot { it == '-' || it == ' ' }.uppercase()

    /** EAN-13 sağlama toplamı: tek konumlar ×1, çift konumlar ×3; toplam 10'un katı olmalı. */
    fun isValidIsbn13(digits: String): Boolean {
        if (digits.length != 13 || !digits.all(Char::isDigit)) return false
        val sum = digits.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return sum % 10 == 0
    }

    /** ISBN-10: ağırlıklar 10..1, son hane 'X' (=10) olabilir; toplam 11'in katı olmalı. */
    fun isValidIsbn10(value: String): Boolean {
        if (value.length != 10) return false
        var sum = 0
        for ((i, c) in value.withIndex()) {
            val digit = when {
                c.isDigit() -> c - '0'
                (c == 'X' || c == 'x') && i == 9 -> 10
                else -> return false
            }
            sum += digit * (10 - i)
        }
        return sum % 11 == 0
    }
}
