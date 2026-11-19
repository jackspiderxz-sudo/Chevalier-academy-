package com.example.core.model

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val englishLabel: String
) {
    TAMIL(code = "ta", nativeLabel = "தமிழ்", englishLabel = "Tamil"),
    ENGLISH(code = "en", nativeLabel = "English", englishLabel = "English"),
    HINDI(code = "hi", nativeLabel = "हिन्दी", englishLabel = "Hindi");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}
