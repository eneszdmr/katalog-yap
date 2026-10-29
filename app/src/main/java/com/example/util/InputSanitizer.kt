package com.example.util

/**
 * Kullanıcı girdilerini XSS, zararlı HTML/script etiketleri, tehlikeli protokoller
 * ve kontrol karakterlerinden temizleyen güvenlik yardımcısı.
 */
object InputSanitizer {

    private val scriptBlockRegex = Regex("(?i)<\\s*script[^>]*>[\\s\\S]*?<\\s*/\\s*script\\s*>")
    private val htmlTagRegex = Regex("<[^>]+>")
    private val dangerousProtocolRegex = Regex("(?i)(javascript|vbscript|data\\s*:\\s*text/html)\\s*:")
    private val inlineEventHandlerRegex = Regex("(?i)\\bon[a-z]+\\s*=")
    private val controlCharsRegex = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")

    /**
     * Metin giriş alanlarında anlık temizleme (yazım sırasında boşlukları korur, zararlı etiketleri siler).
     */
    fun sanitizeLiveInput(input: String, maxLength: Int = 4000): String {
        if (input.isEmpty()) return input
        return input
            .replace(scriptBlockRegex, "")
            .replace(htmlTagRegex, "")
            .replace(dangerousProtocolRegex, "")
            .replace(inlineEventHandlerRegex, "")
            .replace(controlCharsRegex, "")
            .take(maxLength)
    }

    /**
     * Kayıt öncesi tam temizleme (baş/son boşlukları kırpar ve zararlı içerikleri temizler).
     */
    fun sanitizeForStorage(input: String, maxLength: Int = 2000): String {
        return sanitizeLiveInput(input, maxLength).trim()
    }

    /**
     * Telefon numarası temizleme (yalnızca rakam, +, boşluk ve tire karakterlerine izin verir).
     */
    fun sanitizePhone(input: String): String {
        return input.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
            .take(24)
            .trim()
    }

    /**
     * Kısa kod / URL slug temizleme (yalnızca güvenli alfanümerik ve tire karakterleri).
     */
    fun sanitizeSlugOrShortId(input: String): String {
        return sanitizeLiveInput(input, 120)
            .replace(Regex("[^a-zA-Z0-9\\-_/:]"), "")
            .trim()
    }
}
