package io.github.kinsleykajiva.ai.vendors.openai.callbacks;

import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * Session management interface for advanced operations
 */
public interface SessionManager {
	CompletableFuture<Void> updateInstructions(String instructions);
	CompletableFuture<Void> updateVoice(String voice);
	CompletableFuture<Void> createResponse(JSONObject responseConfig);
	CompletableFuture<Void> cancelResponse(String responseId);
	CompletableFuture<Void> clearAudioBuffer();
	CompletableFuture<Void> createConversationItem(JSONObject item, String previousItemId);
	CompletableFuture<Void> retrieveConversationItem(String itemId);
	CompletableFuture<Void> truncateConversationItem(String itemId, int contentIndex, int audioEndMs);
	CompletableFuture<Void> deleteConversationItem(String itemId);
	CompletableFuture<Void> clearOutputAudioBuffer();
}
