package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

public class RateLimitsUpdatedEvent extends RealtimeEvent {
	public RateLimitsUpdatedEvent(JSONObject event) {
		super(event);
	}
	
	public Optional<JSONArray> getRateLimits() {
		return getArray("rate_limits");
	}
}
