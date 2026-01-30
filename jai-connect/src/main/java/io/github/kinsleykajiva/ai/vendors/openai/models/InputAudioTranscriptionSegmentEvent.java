package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class InputAudioTranscriptionSegmentEvent extends RealtimeEvent {
	public InputAudioTranscriptionSegmentEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getContentIndex() {
		return getInt("content_index");
	}
	
	public Optional<String> getText() {
		return getString("text");
	}
	
	public Optional<String> getId() {
		return getString("id");
	}
	
	public Optional<String> getSpeaker() {
		return getString("speaker");
	}
	
	public Optional<Double> getStart() {
		return event.has("start") ? Optional.of(event.getDouble("start")) : Optional.empty();
	}
	
	public Optional<Double> getEnd() {
		return event.has("end") ? Optional.of(event.getDouble("end")) : Optional.empty();
	}
}
