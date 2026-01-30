package io.github.kinsleykajiva.ai.vendors.openai.callbacks;

/**
 * Audio event handler interface
 */
@FunctionalInterface
public interface AudioEventHandler {
	void onAudioEvent(String itemId, int contentIndex, String base64Audio);
}
