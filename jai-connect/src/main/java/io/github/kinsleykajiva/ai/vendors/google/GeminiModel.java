package io.github.kinsleykajiva.ai.vendors.google;

/**
 * Constants for Gemini models.
 * Based on Google AI Studio documentation as of late 2025/2026.
 */
public enum GeminiModel {
    // --- Live API Supported Models ---

    // Gemini 2.5 Flash Live (Native Audio)
    GEMINI_2_5_FLASH_NATIVE_AUDIO_PREVIEW_12_2025("gemini-2.5-flash-native-audio-preview-12-2025", true, true),
    GEMINI_2_5_FLASH_NATIVE_AUDIO_PREVIEW_09_2025("gemini-2.5-flash-native-audio-preview-09-2025", true, true),

    // --- Standard Models (No Live API) ---

    // Gemini 3 Series
    GEMINI_3_PRO_PREVIEW("gemini-3-pro-preview", false, false),
    GEMINI_3_PRO_IMAGE_PREVIEW("gemini-3-pro-image-preview", false, false),
    GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview", false, false),

    // Gemini 2.5 Flash
    GEMINI_2_5_FLASH("gemini-2.5-flash", false, false),
    GEMINI_2_5_FLASH_PREVIEW_09_2025("gemini-2.5-flash-preview-09-2025", false, false),
    GEMINI_2_5_FLASH_IMAGE("gemini-2.5-flash-image", false, false),

    // Gemini 2.5 Flash-Lite
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", false, false),
    GEMINI_2_5_FLASH_LITE_PREVIEW_09_2025("gemini-2.5-flash-lite-preview-09-2025", false, false),

    // Gemini 2.5 Pro
    GEMINI_2_5_PRO("gemini-2.5-pro", false, false),

    // Audio Generation / TTS Only (Not Live API capable as per doc)
    GEMINI_2_5_FLASH_PREVIEW_TTS("gemini-2.5-flash-preview-tts", false, false),
    GEMINI_2_5_PRO_PREVIEW_TTS("gemini-2.5-pro-preview-tts", false, false),

    // Gemini 2.0 (Legacy/Deprecated)
    GEMINI_2_0_FLASH("gemini-2.0-flash", false, false),
    GEMINI_2_0_FLASH_001("gemini-2.0-flash-001", false, false),
    GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", false, false),

    // Kept for backward compat if needed, but marked false for Live if no longer
    // supported
    GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp", false, false);

    private final String modelId;
    private final boolean supportsLiveApi;
    private final boolean nativeAudio;

    GeminiModel(String modelId, boolean supportsLiveApi, boolean nativeAudio) {
        this.modelId = modelId;
        this.supportsLiveApi = supportsLiveApi;
        this.nativeAudio = nativeAudio;
    }

    public String getModelId() {
        return modelId;
    }

    public boolean isSupportsLiveApi() {
        return supportsLiveApi;
    }

    public boolean isNativeAudio() {
        return nativeAudio;
    }
}
