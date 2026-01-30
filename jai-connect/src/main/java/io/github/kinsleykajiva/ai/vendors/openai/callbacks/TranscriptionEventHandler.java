package io.github.kinsleykajiva.ai.vendors.openai.callbacks;

/**
 * Transcription event handler interface
 */
@FunctionalInterface
public interface TranscriptionEventHandler {
	void onTranscription(String itemId, int contentIndex, String transcript);

	/**
	 * Called for incremental transcription deltas (real-time streaming).
	 */
	default void onTranscriptionDelta(String itemId, int contentIndex, String delta) {
	}
}
