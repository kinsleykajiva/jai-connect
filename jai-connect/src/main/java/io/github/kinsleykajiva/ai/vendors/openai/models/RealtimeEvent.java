package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Optional;

/**
 * Base class for all realtime events with type-safe access to common properties
 */
public class RealtimeEvent {
	protected final JSONObject event;
	private final String type;
	private final String eventId;
	
	public RealtimeEvent(JSONObject event) {
		this.event = Objects.requireNonNull(event, "Event cannot be null");
		this.type = event.getString("type");
		this.eventId = event.optString("event_id");
	}
	
	public String getType() {
		return type;
	}
	
	public String getEventId() {
		return eventId;
	}
	
	public JSONObject getRawEvent() {
		return event;
	}
	
	public Optional<String> getString(String key) {
		return Optional.ofNullable(event.optString(key, null));
	}
	
	public Optional<Integer> getInt(String key) {
		return event.has(key) ? Optional.of(event.getInt(key)) : Optional.empty();
	}
	
	public Optional<Boolean> getBoolean(String key) {
		return event.has(key) ? Optional.of(event.getBoolean(key)) : Optional.empty();
	}
	
	public Optional<JSONObject> getObject(String key) {
		return Optional.ofNullable(event.optJSONObject(key));
	}
	
	public Optional<JSONArray> getArray(String key) {
		return Optional.ofNullable(event.optJSONArray(key));
	}
}
