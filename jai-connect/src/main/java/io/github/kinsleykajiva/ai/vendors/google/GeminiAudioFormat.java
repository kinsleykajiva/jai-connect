package io.github.kinsleykajiva.ai.vendors.google;

/**
 * Supported audio formats for Gemini API.
 */
public enum GeminiAudioFormat {
    PCM_16000("audio/pcm;rate=16000"),
    PCM_24000("audio/pcm;rate=24000"),
    PCM_44100("audio/pcm;rate=44100"),
    PCM_48000("audio/pcm;rate=48000");

    private final String mimeType;

    GeminiAudioFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
