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
            // 1. Configuration
            GeminiConfig config = GeminiConfig.builder()
                    .apiKey(apiKey).modelId(GeminiModel.GEMINI_LIVE_2_5_FLASH_NATIVE_AUDIO)
                    .voice(GeminiVoice.FENRIR)
                    .responseAudioFormat(GeminiAudioFormat.PCM_16000)
                    .addResponseModality("AUDIO") // For TTS/STS
                    .build();

            GeminiService service = new GeminiService();

            // 2. Text-to-Speech (TTS)
            System.out.println("--- Testing TTS ---");
            String text = "Hello! This is a test of the enhanced Gemini integration.";
            File outputFile = new File("tts_output.pcm");
            service.textToSpeech(text, config, outputFile);
            System.out.println("TTS Audio saved to: " + outputFile.getAbsolutePath());

            // 3. Speech-to-Text (STT)
            // Assuming we recorded audio or re-using the TTS output for demonstration
            if (outputFile.exists()) {
                System.out.println("--- Testing STT ---");
                byte[] audioData = Files.readAllBytes(outputFile.toPath());

                // For STT only, we might want to ensure we receive text.
                // Note: The model might try to reply (STS) if we send audio.
                // We extract "inputTranscription" to get what we said.
                String transcription = service.speechToText(audioData, config);
                System.out.println("Transcription: " + transcription);
            }

            // 4. Speech-to-Speech (STS) - Live Session
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

            // Send some audio to trigger STS
            if (outputFile.exists()) {
                byte[] audioData = Files.readAllBytes(outputFile.toPath());
                client.sendAudio(audioData, true);
                // Keep alive for a bit to receive response
                Thread.sleep(10000);
            }

            client.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
