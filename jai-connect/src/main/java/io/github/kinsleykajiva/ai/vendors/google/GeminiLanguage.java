package io.github.kinsleykajiva.ai.vendors.google;

/**
 * Supported languages for Gemini Live API.
 * <p>
 * Languages marked with {@code supportsNativeAudio = false} are not available
 * for Native audio models.
 */
public enum GeminiLanguage {
    // Supported for Native Audio
    GERMAN_GERMANY("de-DE", true),
    ENGLISH_US("en-US", true),
    ENGLISH_UK("en-GB", false), // Marked with * in user request
    ENGLISH_AUSTRALIA("en-AU", false), // Marked with *
    ENGLISH_INDIA("en-IN", true),
    SPANISH_US("es-US", true),
    SPANISH_SPAIN("es-ES", false), // Marked with *
    FRENCH_FRANCE("fr-FR", true),
    FRENCH_CANADA("fr-CA", false), // Marked with *
    PORTUGUESE_BRAZIL("pt-BR", true),
    INDONESIAN_INDONESIA("id-ID", true),
    ITALIAN_ITALY("it-IT", true),
    JAPANESE_JAPAN("ja-JP", true),
    TURKISH_TURKEY("tr-TR", true),
    VIETNAMESE_VIETNAM("vi-VN", true),
    BENGALI_INDIA("bn-IN", true),
    GUJARATI_INDIA("gu-IN", false), // Marked with *
    KANNADA_INDIA("kn-IN", false), // Marked with *
    MARATHI_INDIA("mr-IN", true),
    MALAYALAM_INDIA("ml-IN", false), // Marked with *
    TAMIL_INDIA("ta-IN", true),
    TELUGU_INDIA("te-IN", true),
    DUTCH_NETHERLANDS("nl-NL", true),
    KOREAN_SOUTH_KOREA("ko-KR", true),
    MANDARIN_CHINESE_CHINA("cmn-CN", false), // Marked with *
    POLISH_POLAND("pl-PL", true),
    RUSSIAN_RUSSIA("ru-RU", true),
    THAI_THAILAND("th-TH", true),
    HINDI_INDIA("hi-IN", true),
    ARABIC_GENERIC("ar-XA", true);

    private final String languageCode;
    private final boolean supportsNativeAudio;

    GeminiLanguage(String languageCode, boolean supportsNativeAudio) {
        this.languageCode = languageCode;
        this.supportsNativeAudio = supportsNativeAudio;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public boolean isSupportsNativeAudio() {
        return supportsNativeAudio;
    }
}
