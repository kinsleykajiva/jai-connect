package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ConversationItemAddedEvent extends RealtimeEvent {
	public ConversationItemAddedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getItem() {
		return getObject("item");
	}
	
	public Optional<String> getPreviousItemId() {
		return getString("previous_item_id");
	}
}
