package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

public class InputAudioTranscriptionDeltaEvent extends RealtimeEvent {
	public InputAudioTranscriptionDeltaEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getContentIndex() {
		return getInt("content_index");
	}
	
	public Optional<String> getDelta() {
		return getString("delta");
	}
	
	public Optional<String> getObfuscation() {
		return getString("obfuscation");
	}
	
	public Optional<JSONArray> getLogprobs() {
		return getArray("logprobs");
	}
}
