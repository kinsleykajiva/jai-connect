package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ConversationItemRetrievedEvent extends RealtimeEvent {
	public ConversationItemRetrievedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getItem() {
		return getObject("item");
	}
}
