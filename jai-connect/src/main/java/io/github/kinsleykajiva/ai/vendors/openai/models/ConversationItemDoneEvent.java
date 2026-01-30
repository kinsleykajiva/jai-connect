package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ConversationItemDoneEvent extends RealtimeEvent {
	public ConversationItemDoneEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONObject> getItem() {
		return getObject("item");
	}
	
	public Optional<String> getPreviousItemId() {
		return getString("previous_item_id");
	}
}
