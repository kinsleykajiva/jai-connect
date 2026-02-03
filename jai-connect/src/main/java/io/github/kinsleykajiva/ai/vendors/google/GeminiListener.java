package io.github.kinsleykajiva.ai.vendors.google;

public interface GeminiListener {
	void onAudio(byte[] audioData);
	void onTurnComplete();
	void onTranscription(String transcription);
}
