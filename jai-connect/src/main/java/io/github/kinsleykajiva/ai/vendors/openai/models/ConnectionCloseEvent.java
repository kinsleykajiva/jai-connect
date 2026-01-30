package io.github.kinsleykajiva.ai.vendors.openai.models;

/**
 * Connection close event with status information
 */
public record ConnectionCloseEvent(int statusCode, String reason, Throwable cause) {
	public ConnectionCloseEvent(int statusCode, String reason) {
		this(statusCode, reason, null);
	}
	
	@Override
	public String toString() {
		return String.format("ConnectionCloseEvent{status=%d, reason='%s'}", statusCode, reason);
	}
}
