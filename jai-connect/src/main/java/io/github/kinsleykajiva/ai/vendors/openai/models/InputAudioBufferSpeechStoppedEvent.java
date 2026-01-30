package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class InputAudioBufferSpeechStoppedEvent extends RealtimeEvent {
	public InputAudioBufferSpeechStoppedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getAudioEndMs() {
		return getInt("audio_end_ms");
	}
}
