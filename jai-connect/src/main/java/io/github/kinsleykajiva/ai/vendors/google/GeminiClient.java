package io.github.kinsleykajiva.ai.vendors.google;

import com.google.genai.AsyncSession;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

public class GeminiClient {

	private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
	private final StringBuilder outputTranscription = new StringBuilder();
	private final StringBuilder inputTranscription = new StringBuilder();
	private final AsyncSession session;
	private final List<GeminiListener> listeners = new CopyOnWriteArrayList<>();

	public GeminiClient(GeminiConfig config) throws ExecutionException, InterruptedException {

		if (Objects.isNull(config)) {
			throw new IllegalArgumentException("GeminiConfig cannot be null");
		}

		if (Objects.isNull(config.getApiKey()) || config.getApiKey().isEmpty()) {
			throw new IllegalArgumentException("API Key cannot be null or empty");
		}

		// System instruction is optional, but if present we typically use it.

		Client client = Client.builder().apiKey(config.getApiKey()).build();
		List<Modality> responseModalities = new ArrayList<>();

		// Map string modalities to Modality objects
		for (String mod : config.getResponseModalities()) {
			if ("AUDIO".equalsIgnoreCase(mod)) {
				responseModalities.add(new Modality(Modality.Known.AUDIO));
			} else if ("TEXT".equalsIgnoreCase(mod)) {
				responseModalities.add(new Modality(Modality.Known.TEXT));
			}
		}
		if (responseModalities.isEmpty()) {
			responseModalities.add(new Modality(Modality.Known.AUDIO));
		}

		PrebuiltVoiceConfig prebuiltVoiceConfig = PrebuiltVoiceConfig.builder().voiceName(config.getVoice().getValue())
				.build();
		VoiceConfig voiceConfig = VoiceConfig.builder().prebuiltVoiceConfig(prebuiltVoiceConfig).build();

		List<Part> parts = new ArrayList<>();
		if (config.getSystemInstruction() != null && !config.getSystemInstruction().isEmpty()) {
			parts.add(Part.fromText(config.getSystemInstruction()));
		}
		Content systemInstruction = parts.isEmpty() ? null : Content.builder().parts(parts).build();

		AudioTranscriptionConfig audioTranscriptionConfig = AudioTranscriptionConfig.builder().build();

		LiveConnectConfig liveConnectConfig = LiveConnectConfig.builder()
				.responseModalities(responseModalities)
				.systemInstruction(systemInstruction)
				.outputAudioTranscription(audioTranscriptionConfig)
				.inputAudioTranscription(audioTranscriptionConfig)
				.speechConfig(
						SpeechConfig.builder()
								.voiceConfig(voiceConfig)
								.languageCode(config.getLanguage().getLanguageCode())
								.build())
				.build();

		logger.info("Connecting to Gemini Live API...");

		session = client.async.live.connect(config.getModelId(), liveConnectConfig).get();
		logger.info("Connected....");
		session.receive(this::handleAudioResponse);
		logger.info("Receive stream started....");
	}

	/** Callback function to handle incoming audio messages from the server. */
	private void handleAudioResponse(LiveServerMessage message) {
		logger.info("Received audio from Gemini");
		message
				.serverContent()
				.ifPresent(
						content -> {
							content.outputTranscription()
									.ifPresent(transcription -> {
										if (transcription.text().isEmpty()) {
											return;
										}
										logger.info("Buffering transcription part: {}", transcription.text().get());
										transcription.text().ifPresent(outputTranscription::append);
										flushBufferOnPunctuation(outputTranscription, "outputTranscription", listeners);
									});
							// now do input transcription
							content.inputTranscription().ifPresent(transcription -> {
								if (transcription.text().isEmpty()) {
									return;
								}
								logger.info("Buffering input transcription part: {}", transcription.text().get());
								transcription.text().ifPresent(inputTranscription::append);
								flushBufferOnPunctuation(inputTranscription, "inputTranscription", listeners);
							});

							if (content.turnComplete().orElse(false)) {

								String output = outputTranscription.toString().trim();
								String input = inputTranscription.toString().trim();

								if (!output.isEmpty() || !input.isEmpty()) {
									JSONObject transcriptionJson = new JSONObject();
									transcriptionJson.put("outputTranscription", output);
									transcriptionJson.put("inputTranscription", input);

									String fullTranscription = transcriptionJson.toString();

									logger.info("Sending final transcription: {}", fullTranscription);
									listeners.forEach(listener -> listener.onTranscription(fullTranscription));
								}

								outputTranscription.setLength(0); // Clear buffer
								inputTranscription.setLength(0); // Clear buffer

								// When interrupted, Gemini sends a turn_complete.Stop the speaker if the turn
								// is complete.
								logger.info("Gemini turn complete.");
								listeners.forEach(GeminiListener::onTurnComplete);

							} else {
								content.modelTurn().stream()
										.flatMap(modelTurn -> modelTurn.parts().stream())
										.flatMap(Collection::stream)

										.map(part -> part.inlineData().flatMap(Blob::data))
										.flatMap(Optional::stream)
										.forEach(audioBytes -> listeners
												.forEach(listener -> listener.onAudio(audioBytes)));
							}
						});
	}

	private void flushBufferOnPunctuation(StringBuilder buffer, String key, List<GeminiListener> listeners) {
		String text = buffer.toString();
		int lastPunctuationIndex = -1;

		// Find the last index of any sentence-ending punctuation
		lastPunctuationIndex = Math.max(lastPunctuationIndex, text.lastIndexOf('.'));
		lastPunctuationIndex = Math.max(lastPunctuationIndex, text.lastIndexOf('?'));
		lastPunctuationIndex = Math.max(lastPunctuationIndex, text.lastIndexOf('!'));
		lastPunctuationIndex = Math.max(lastPunctuationIndex, text.lastIndexOf(','));

		if (lastPunctuationIndex != -1) {
			// Extract the sentence(s) up to the last punctuation mark.
			String sentenceToSend = text.substring(0, lastPunctuationIndex + 1).trim();

			if (!sentenceToSend.isEmpty()) {
				JSONObject transcriptionJson = new JSONObject();
				// Ensure the other key exists but is empty, so the frontend doesn't break.
				String otherKey = key.equals("outputTranscription") ? "inputTranscription" : "outputTranscription";
				transcriptionJson.put(key, sentenceToSend);
				transcriptionJson.put(otherKey, "");

				String jsonString = transcriptionJson.toString();
				logger.info("Sending partial transcription: {}", jsonString);
				listeners.forEach(listener -> listener.onTranscription(jsonString));

				// Remove the sent part from the buffer.
				buffer.delete(0, lastPunctuationIndex + 1);
			}
		}
	}

	/**
	 * Sends audio data to the Gemini API.
	 *
	 * @param audioData the audio data to send
	 */
	public void sendAudio(byte[] audioData) {
		sendAudio(audioData, false);
	}

	public void sendAudio(byte[] audioData, boolean endOfTurn) {
		if (session != null) {
			try {
				LiveSendRealtimeInputParameters parameters = LiveSendRealtimeInputParameters.builder()
						.media(Blob.builder().mimeType("audio/pcm;rate=16000").data(audioData).build())
						.build();
				session.sendRealtimeInput(parameters);

				if (endOfTurn) {
					// Send turnComplete using client content
					LiveSendClientContentParameters endParams = LiveSendClientContentParameters.builder()
							.turnComplete(true)
							.turns(List.of(Content.builder().parts(List.of(Part.fromText(""))).build())) // Empty text
																											// part
							.build();
					session.sendClientContent(endParams);
				}
			} catch (Exception e) {
				logger.error("Error sending audio to Gemini: {}", e.getMessage());
			}
		}
	}

	public void clearListeners() {
		listeners.clear();
	}

	/**
	 * Sends text to the Gemini API.
	 *
	 * @param text the text to send
	 */
	public void sendText(String text) {
		if (session != null) {
			try {
				Content content = Content.builder().parts(List.of(Part.fromText(text))).build();

				LiveSendClientContentParameters parameters = LiveSendClientContentParameters.builder()
						.turnComplete(true)
						.turns(List.of(content))
						.build();

				session.sendClientContent(parameters);
			} catch (Exception e) {
				logger.error("Error sending text to Gemini: {}", e.getMessage());
			}
		}
	}

	public void addListener(GeminiListener listener) {
		listeners.add(listener);
	}

	public void removeListener(GeminiListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Closes the Gemini session.
	 */
	public void close() {
		if (session != null) {
			session.close();
			logger.info("Gemini session closed.");
		}
	}
}
