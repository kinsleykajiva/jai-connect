package io.github.kinsleykajiva.ai.vendors.google;

/**
 * Constants for Gemini models.
 */
public enum GeminiModel {
    // Live API Supported
    GEMINI_LIVE_2_5_FLASH_NATIVE_AUDIO("gemini-live-2.5-flash-native-audio", true, true),
    GEMINI_LIVE_2_5_FLASH_PREVIEW_09_2025("gemini-live-2.5-flash-preview-native-audio-09-2025", true, true),
    GEMINI_2_0_FLASH_LIVE_PREVIEW_04_09("gemini-2.0-flash-live-preview-04-09", true, false),

    // Text/Multimodal only (No Live API)
    GEMINI_2_0_FLASH("gemini-2.0-flash", false, false),
    GEMINI_3_PRO_PREVIEW("gemini-3-pro-preview", false, false),
    GEMINI_2_5_PRO("gemini-2.5-pro", false, false),
    GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview", false, false),
    GEMINI_2_5_FLASH("gemini-2.5-flash", false, false),

    // Legacy/Exp
    GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp", true, false);

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
