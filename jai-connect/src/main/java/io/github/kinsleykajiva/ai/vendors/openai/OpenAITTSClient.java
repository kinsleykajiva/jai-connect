package io.github.kinsleykajiva.ai.vendors.openai;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for OpenAI's Text-to-Speech (TTS) REST API.
 * Uses the /v1/audio/speech endpoint.
 */
public class OpenAITTSClient {
    private static final Logger logger = LoggerFactory.getLogger(OpenAITTSClient.class);
    private static final String API_URL = "https://api.openai.com/v1/audio/speech";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final OpenAIConfig config;
    private final HttpClient httpClient;

    public OpenAITTSClient(OpenAIConfig config) {
        this.config = Objects.requireNonNull(config, "OpenAIConfig cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    /**
     * Generates speech from input text and returns the raw audio bytes.
     *
     * @param input the text to generate speech for
     * @param voice the voice to use (e.g., "alloy", "coral")
     * @param model the model to use (e.g., "gpt-4o-mini-tts", "tts-1")
     * @return a CompletableFuture containing the audio bytes
     */
    public CompletableFuture<byte[]> generateSpeech(String input, String voice, String model) {
        return generateSpeech(input, voice, model, null, null);
    }

    /**
     * Generates speech from input text with optional instructions.
     *
     * @param input        the text to generate speech for
     * @param voice        the voice to use
     * @param model        the model to use
     * @param instructions optional instructions (supported by gpt-4o-mini-tts)
     * @param format       optional response format (e.g., "mp3", "wav")
     * @return a CompletableFuture containing the audio bytes
     */
    public CompletableFuture<byte[]> generateSpeech(String input, String voice, String model, String instructions,
            String format) {
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("input", input)
                .put("voice", voice);

        if (instructions != null && !instructions.isEmpty()) {
            body.put("instructions", instructions);
        }
        if (format != null && !format.isEmpty()) {
            body.put("response_format", format);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(DEFAULT_TIMEOUT)
                .build();

        logger.debug("Sending TTS request to OpenAI: {} with voice {}", model, voice);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        String errorBody = new String(response.body());
                        logger.error("TTS request failed with status {}: {}", response.statusCode(), errorBody);
                        throw new RuntimeException("TTS request failed: " + errorBody);
                    }
                    return response.body();
                });
    }

    /**
     * Generates speech and saves it to a file.
     * If targetPath is null, it saves to the current directory with a random name.
     *
     * @param input      the text to generate speech for
     * @param voice      the voice to use
     * @param model      the model to use
     * @param targetPath optional target path. If it's a directory or null, a random
     *                   filename is generated.
     * @return a CompletableFuture containing the Path to the saved file
     */
    public CompletableFuture<Path> generateSpeechToFile(String input, String voice, String model, Path targetPath) {
        return generateSpeech(input, voice, model)
                .thenApply(audioBytes -> {
                    try {
                        Path finalPath = resolveTargetPath(targetPath, "mp3");
                        Files.write(finalPath, audioBytes);
                        logger.info("Speech generated and saved to: {}", finalPath.toAbsolutePath());
                        return finalPath;
                    } catch (IOException e) {
                        logger.error("Failed to save speech to file", e);
                        throw new RuntimeException("Failed to save audio to file", e);
                    }
                });
    }

    private Path resolveTargetPath(Path targetPath, String extension) {
        if (targetPath == null) {
            return Paths.get("speech_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension);
        }

        if (Files.isDirectory(targetPath)) {
            return targetPath.resolve("speech_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension);
        }

        // If it doesn't have an extension, add one
        String fileName = targetPath.getFileName().toString();
        if (!fileName.contains(".")) {
            return Paths.get(targetPath.toString() + "." + extension);
        }

        return targetPath;
    }
}
