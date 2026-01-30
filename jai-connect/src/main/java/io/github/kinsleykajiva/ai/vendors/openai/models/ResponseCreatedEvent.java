package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ResponseCreatedEvent extends RealtimeEvent {
	public ResponseCreatedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getResponse() {
		return getObject("response");
	}
}
