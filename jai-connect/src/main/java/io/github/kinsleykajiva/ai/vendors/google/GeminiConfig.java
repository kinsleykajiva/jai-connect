package io.github.kinsleykajiva.ai.vendors.google;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for Gemini API.
 */
public class GeminiConfig {
    // Required
    private final String apiKey;
    private final String modelId;

    // Optional with defaults
    private final GeminiVoice voice;
    private final GeminiAudioFormat responseAudioFormat;
    private final String systemInstruction;
    private final List<String> responseModalities; // "AUDIO", "TEXT"
    private final GeminiLanguage language;

    private GeminiConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.modelId = builder.modelId;
        this.voice = builder.voice;
        this.responseAudioFormat = builder.responseAudioFormat;
        this.systemInstruction = builder.systemInstruction;
        this.responseModalities = builder.responseModalities;
        this.language = builder.language;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelId() {
        return modelId;
    }

    public GeminiVoice getVoice() {
        return voice;
    }

    public GeminiAudioFormat getResponseAudioFormat() {
        return responseAudioFormat;
    }

    public String getSystemInstruction() {
        return systemInstruction;
    }

    public List<String> getResponseModalities() {
        return responseModalities;
    }

    public GeminiLanguage getLanguage() {
        return language;
    }

    // Compatibility accessors for existing code (if any relying on record style
    // accessors)
    public String apiKey() {
        return apiKey;
    }

    public String modelId() {
        return modelId;
    }

    public String voiceIdOrName() {
        return voice.getValue();
    }

    public String part() {
        return systemInstruction;
    } // Mapped 'part' to systemInstruction

    public String languageCode() {
        return language.getLanguageCode();
    }

    public static class Builder {
        private String apiKey;
        // Default to a model that supports Live API
        private String modelId = GeminiModel.GEMINI_2_0_FLASH_LIVE_PREVIEW_04_09.getModelId();
        private GeminiVoice voice = GeminiVoice.PUCK;
        private GeminiAudioFormat responseAudioFormat = GeminiAudioFormat.PCM_16000;
        private String systemInstruction = "You are a helpful assistant.";
        private List<String> responseModalities = new ArrayList<>();
        private GeminiLanguage language = GeminiLanguage.ENGLISH_US;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder modelId(GeminiModel model) {
            this.modelId = model.getModelId();
            return this;
        }

        public Builder voice(GeminiVoice voice) {
            this.voice = voice;
            return this;
        }

        public Builder responseAudioFormat(GeminiAudioFormat format) {
            this.responseAudioFormat = format;
            return this;
        }

        public Builder systemInstruction(String systemInstruction) {
            this.systemInstruction = systemInstruction;
            return this;
        }

        public Builder addResponseModality(String modality) {
            this.responseModalities.add(modality);
            return this;
        }

        public Builder language(GeminiLanguage language) {
            this.language = language;
            return this;
        }

        /**
         * @deprecated Use {@link #language(GeminiLanguage)} instead.
         */
        @Deprecated
        public Builder languageCode(String languageCode) {
            // Best effort mapping or null?
            // For now, let's just log a warning or ignore if we strictly enforce Enum.
            // Or try to find enum by code.
            for (GeminiLanguage lang : GeminiLanguage.values()) {
                if (lang.getLanguageCode().equalsIgnoreCase(languageCode)) {
                    this.language = lang;
                    return this;
                }
            }
            // If not found, default to US or previous?
            return this;
        }

        public GeminiConfig build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("API Key is required");
            }
            if (responseModalities.isEmpty()) {
                responseModalities.add("AUDIO"); // Default
            }
            return new GeminiConfig(this);
        }
    }
}
