package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class InputAudioBufferCommittedEvent extends RealtimeEvent {
	public InputAudioBufferCommittedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<String> getPreviousItemId() {
		return getString("previous_item_id");
	}
}
