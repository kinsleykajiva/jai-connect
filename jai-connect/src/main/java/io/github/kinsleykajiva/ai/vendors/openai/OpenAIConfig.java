package io.github.kinsleykajiva.ai.vendors.openai;

public record OpenAIConfig(
        String tcpResourceLink,
        String apiKey,
        AudioFormats inputAudioFormat,
        AudioFormats outputAudioFormat,
        RealtimeMode mode,
        String transcriptionModel) {
    public OpenAIConfig {
        if (mode == null)
            mode = RealtimeMode.CONVERSATION;
        if (transcriptionModel == null)
            transcriptionModel = OpenAIModels.WHISPER_1;
    }
}
