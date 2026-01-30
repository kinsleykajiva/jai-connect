package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

import java.util.Optional;

public class ConversationItemTruncatedEvent extends RealtimeEvent {
	public ConversationItemTruncatedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<String> getItemId() {
		return getString("item_id");
	}
	
	public Optional<Integer> getContentIndex() {
		return getInt("content_index");
	}
	
	public Optional<Integer> getAudioEndMs() {
		return getInt("audio_end_ms");
	}
}
