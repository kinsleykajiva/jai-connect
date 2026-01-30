package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class SessionCreatedEvent extends RealtimeEvent {
	public SessionCreatedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getSession() {
		return getObject("session");
	}
}
