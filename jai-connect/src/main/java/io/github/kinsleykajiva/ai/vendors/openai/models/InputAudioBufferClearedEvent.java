package io.github.kinsleykajiva.ai.vendors.openai.models;

import org.json.JSONObject;

public class InputAudioBufferClearedEvent extends RealtimeEvent {
	public InputAudioBufferClearedEvent(JSONObject event) {
		super(event);
	}
}
