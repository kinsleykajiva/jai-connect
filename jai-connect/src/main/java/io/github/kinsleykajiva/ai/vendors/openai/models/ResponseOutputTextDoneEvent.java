package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ResponseOutputTextDoneEvent extends RealtimeEvent {
	public ResponseOutputTextDoneEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getResponseId() {
		return getString("response_id");
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getOutputIndex() {
		return getInt("output_index");
	}
	
	public Optional<Integer> getContentIndex() {
		return getInt("content_index");
	}
	
	public Optional<String> getText() {
		return getString("text");
	}
}
