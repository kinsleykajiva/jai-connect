package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ResponseOutputItemAddedEvent extends RealtimeEvent {
	public ResponseOutputItemAddedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getResponseId() {
		return getString("response_id");
	}
	
	public Optional<Integer> getOutputIndex() {
		return getInt("output_index");
	}
	
	public Optional<JSONObject> getItem() {
		return getObject("item");
	}
}
