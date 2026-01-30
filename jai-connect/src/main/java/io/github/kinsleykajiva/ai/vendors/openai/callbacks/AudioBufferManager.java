package io.github.kinsleykajiva.ai.vendors.openai.callbacks;

import java.util.concurrent.CompletableFuture;

/**
 * Audio buffer management interface
 */
public interface AudioBufferManager {
	CompletableFuture<Void> appendAudio(String base64Audio);
	CompletableFuture<Void> commitBuffer();
	CompletableFuture<Void> clearBuffer();
	long getCurrentDurationMs();
}
