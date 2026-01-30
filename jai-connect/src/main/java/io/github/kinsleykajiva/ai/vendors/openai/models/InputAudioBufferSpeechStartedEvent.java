package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class InputAudioBufferSpeechStartedEvent extends RealtimeEvent {
	public InputAudioBufferSpeechStartedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getAudioStartMs() {
		return getInt("audio_start_ms");
	}
}
