package io.github.kinsleykajiva.demo;

import io.github.kinsleykajiva.ai.vendors.google.*;
import java.io.File;
import java.nio.file.Files;

public class GeminiEnhancedDemo {

    public static void main(String[] args) {
         String apiKey = System.getenv("GEMINI_API_KEY");
      

        if (apiKey == null) {
            System.err.println("Please set GEMINI_API_KEY environment variable.");
            return;
        }

        try {
            //  Configuration
            GeminiConfig config = GeminiConfig.builder()
                    .apiKey(apiKey).modelId(GeminiModel.GEMINI_2_5_FLASH_NATIVE_AUDIO_PREVIEW_12_2025)
                    .voice(GeminiVoice.FENRIR)
                    .responseAudioFormat(GeminiAudioFormat.PCM_16000)
                    .addResponseModality("AUDIO") // For TTS/STS
                    .build();

            GeminiService service = new GeminiService();

            //  Speech-to-Speech (STS) - Live Session
            System.out.println("--- Testing STS (Live Session) ---");
            GeminiClient client = service.startLiveSession(config, new GeminiListener() {
                @Override
                public void onAudio(byte[] audioData) {
                    System.out.println("Received audio response chunk: " + audioData.length + " bytes");
                }

                @Override
                public void onTurnComplete() {
                    System.out.println("Turn complete.");
                }

                @Override
                public void onTranscription(String transcription) {
                    System.out.println("Live Transcription: " + transcription);
                }
            });

            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
