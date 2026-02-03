package io.github.kinsleykajiva.ai.vendors.google;

/**
 * Prebuilt voices for Gemini API.
 */
public enum GeminiVoice {
    PUCK("Puck"),
    CHARON("Charon"),
    AMBER("Amber"),
    AOEDE("Aoede"),
    KORE("Kore"),
    FENRIR("Fenrir");

    private final String value;

    GeminiVoice(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GeminiVoice fromString(String text) {
        for (GeminiVoice b : GeminiVoice.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return PUCK; // default
    }
}
