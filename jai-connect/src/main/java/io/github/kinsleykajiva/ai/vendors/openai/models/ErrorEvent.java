package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

// Type-specific event classes
public class ErrorEvent extends RealtimeEvent {
	public ErrorEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getError() {
		return getObject("error");
	}
}
