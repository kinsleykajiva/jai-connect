package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class SessionUpdatedEvent extends RealtimeEvent {
	public SessionUpdatedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getSession() {
		return getObject("session");
	}
}
