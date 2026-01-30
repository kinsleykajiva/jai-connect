package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

public class InputAudioTranscriptionCompletedEvent extends RealtimeEvent {
	public InputAudioTranscriptionCompletedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getContentIndex() {
		return getInt("content_index");
	}
	
	public Optional<String> getTranscript() {
		return getString("transcript");
	}
	
	public Optional<JSONObject> getUsage() {
		return getObject("usage");
	}
	
	public Optional<JSONArray> getLogprobs() {
		return getArray("logprobs");
	}
}
