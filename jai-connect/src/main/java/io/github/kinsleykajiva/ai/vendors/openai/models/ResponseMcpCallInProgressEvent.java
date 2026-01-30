package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ResponseMcpCallInProgressEvent extends RealtimeEvent {
	public ResponseMcpCallInProgressEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getOutputIndex() {
		return getInt("output_index");
	}
}
