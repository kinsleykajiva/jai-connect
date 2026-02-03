package io.github.kinsleykajiva.ai.vendors.google;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service generic class to handle Gemini logic.
 */
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    /**
     * Converts text to speech using Gemini API.
     *
     * @param text   The text to convert.
     * @param config The configuration for Gemini (API key, voice, etc.).
     * @return The audio data as a byte array.
     * @throws Exception If an error occurs.
     */
    public byte[] textToSpeech(String text, GeminiConfig config) throws Exception {
        CompletableFuture<byte[]> futureAudio = new CompletableFuture<>();
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        // Ensure we are in AUDIO mode
        GeminiConfig ttsConfig = config;
        if (!config.getResponseModalities().contains("AUDIO")) {
            // If audio not requested, we can't do TTS.
        }

        // Validate model supports Live API
        validateLiveApiSupport(config);

        GeminiClient client = new GeminiClient(config);

        client.addListener(new GeminiListener() {
            @Override
            public void onAudio(byte[] audioData) {
                try {
                    audioBuffer.write(audioData);
                } catch (IOException e) {
                    futureAudio.completeExceptionally(e);
                }
            }

            @Override
            public void onTurnComplete() {
                logger.info("TTS Turn complete. Total bytes: {}", audioBuffer.size());
                futureAudio.complete(audioBuffer.toByteArray());
                // We can close the client here as we are done for this single request
                client.close();
            }

            @Override
            public void onTranscription(String transcription) {
                // Ignore for TTS
            }
        });

        // Send the text
        client.sendText(text);

        try {
            // Wait for completion with a timeout
            return futureAudio.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            client.close();
            throw new TimeoutException("TTS request timed out");
        } catch (Exception e) {
            client.close();
            throw e;
        }
    }

    /**
     * Converts text to speech and saves to a file.
     *
     * @param text       The text to convert.
     * @param config     The configuration for Gemini.
     * @param outputFile The file to write the audio to.
     * @throws Exception If an error occurs.
     */
    public void textToSpeech(String text, GeminiConfig config, File outputFile) throws Exception {
        byte[] audioData = textToSpeech(text, config);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(audioData);
            logger.info("Saved TTS audio to: {}", outputFile.getAbsolutePath());
        }
    }

    /**
     * Converts speech to text using Gemini API.
     *
     * @param audioData The audio data.
     * @param config    The configuration.
     * @return The transcription of the audio.
     * @throws Exception If an error occurs.
     */
    public String speechToText(byte[] audioData, GeminiConfig config) throws Exception {
        CompletableFuture<String> futureText = new CompletableFuture<>();
        final StringBuilder capturedText = new StringBuilder();

        // Validate model supports Live API
        validateLiveApiSupport(config);

        GeminiClient client = new GeminiClient(config);

        client.addListener(new GeminiListener() {
            @Override
            public void onAudio(byte[] audio) {
                // Ignore audio output for STT
            }

            @Override
            public void onTurnComplete() {
                client.close();
                futureText.complete(capturedText.toString().trim());
            }

            @Override
            public void onTranscription(String transcription) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(transcription);
                    if (json.has("inputTranscription")) {
                        String text = json.getString("inputTranscription");
                        if (!text.isEmpty()) {
                            capturedText.append(text).append(" ");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing transcription", e);
                }
            }
        });

        client.sendAudio(audioData, true);

        try {
            return futureText.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            client.close();
            throw new TimeoutException("STT request timed out");
        } catch (Exception e) {
            client.close();
            throw e;
        }
    }

    /**
     * Starts a live Speech-to-Speech session.
     *
     * @param config   The configuration.
     * @param listener The listener for events.
     * @return The client instance.
     * @throws Exception If an error occurs.
     */
    public GeminiClient startLiveSession(GeminiConfig config, GeminiListener listener) throws Exception {
        validateLiveApiSupport(config);
        GeminiClient client = new GeminiClient(config);
        client.addListener(listener);
        return client;
    }

    private void validateLiveApiSupport(GeminiConfig config) {
        String modelId = config.getModelId();
        for (GeminiModel model : GeminiModel.values()) {
            if (model.getModelId().equals(modelId)) {
                if (!model.isSupportsLiveApi()) {
                    throw new IllegalArgumentException(
                            "The selected model '" + modelId + "' does not support the Gemini Live API.");
                }

                // Native Audio Check
                if (model.isNativeAudio()) {
                    if (config.getLanguage() != null && !config.getLanguage().isSupportsNativeAudio()) {
                        throw new IllegalArgumentException(
                                "The selected language '" + config.getLanguage() + "' ("
                                        + config.getLanguage().getLanguageCode() +
                                        ") is not supported by the Native Audio model '" + modelId + "'.");
                    }
                }
                return;
            }
        }
        // If unknown model, we might warn or just allow it (assuming it might be a new
        // one)
        logger.warn("Unknown model ID: {}. Unable to verify Live API support.", modelId);
    }
}
