package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class InputAudioBufferTimeoutTriggeredEvent extends RealtimeEvent {
	public InputAudioBufferTimeoutTriggeredEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getAudioStartMs() {
		return getInt("audio_start_ms");
	}
	
	public Optional<Integer> getAudioEndMs() {
		return getInt("audio_end_ms");
	}
}
