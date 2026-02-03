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

public class GeminiWebSocketClient {
	
	private static final Logger logger = LoggerFactory.getLogger(GeminiWebSocketClient.class);
	private final StringBuilder outputTranscription = new StringBuilder();
	private final StringBuilder inputTranscription = new StringBuilder();
	private final AsyncSession session;
	private final List<GeminiListener> listeners = new CopyOnWriteArrayList<>();
	
	public GeminiWebSocketClient(GeminiConfig config) throws ExecutionException, InterruptedException {
		
		if(Objects.isNull(config)){
			throw new IllegalArgumentException("GeminiConfig cannot be null");
		}
		
		if(Objects.isNull(config.apiKey()) || config.apiKey().isEmpty()){
			throw new IllegalArgumentException("API Key cannot be null or empty");
		}
		
		if(config.part() == null || config.part().isEmpty()){
			throw new IllegalArgumentException("Part cannot be null or empty");
		}
		
		
		Client client = Client.builder().apiKey(config.apiKey()).build();
		List<Modality> responseModalities = new ArrayList<>();
		
		
		responseModalities.add(new Modality(
				Modality.Known.AUDIO
		));
		PrebuiltVoiceConfig prebuiltVoiceConfig  = PrebuiltVoiceConfig.builder().voiceName(config.voiceIdOrName()).build();
		VoiceConfig voiceConfig = VoiceConfig.builder().prebuiltVoiceConfig(prebuiltVoiceConfig).build();
		Part part = Part.fromText(config.part());
		Content systemInstruction                         = Content.builder().parts(List.of(part)).build();
		AudioTranscriptionConfig audioTranscriptionConfig = AudioTranscriptionConfig.builder().build();
		
		LiveConnectConfig liveConnectConfig =
				LiveConnectConfig.builder()
						.responseModalities(responseModalities)
						.systemInstruction(systemInstruction)
						.outputAudioTranscription(audioTranscriptionConfig)
						.inputAudioTranscription(audioTranscriptionConfig)
						.speechConfig(
								SpeechConfig.builder()
										.voiceConfig(voiceConfig)
										.languageCode(Objects.isNull(config.languageCode()) || config.languageCode().isEmpty()? "en-US":config.languageCode())
										.build())
						.build();
		
		logger.info("Connecting to Gemini Live API...");
		
		session = client.async.live.connect(config.modelId(), liveConnectConfig).get();
		logger.info("Connected....");
		session.receive(this::handleAudioResponse);
		logger.info("Receive stream started....");
	}
	
	/** Callback function to handle incoming audio messages from the server. */
	private void handleAudioResponse(LiveServerMessage message) {
		logger.info("Received audio from Gemini" );
		message
				.serverContent()
				.ifPresent(
						content -> {
							content.outputTranscription()
									.ifPresent(transcription -> {
										if(transcription.text().isEmpty()){
											return;
										}
										logger.info("Buffering transcription part: {}", transcription.text().get());
										transcription.text().ifPresent(outputTranscription::append);
										flushBufferOnPunctuation(outputTranscription, "outputTranscription", listeners);
									});
							// now do input transcription
							content.inputTranscription().ifPresent(transcription -> {
								if(transcription.text().isEmpty()){
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
								
								// When interrupted, Gemini sends a turn_complete.Stop the speaker if the turn is complete.
								logger.info("Gemini turn complete.");
								listeners.forEach(GeminiListener::onTurnComplete);
								
							} else {
								content.modelTurn().stream()
										.flatMap(modelTurn -> modelTurn.parts().stream())
										.flatMap(Collection::stream)
										
										.map(part -> part.inlineData().flatMap(Blob::data))
										.flatMap(Optional::stream)
										.forEach(audioBytes -> listeners.forEach(listener -> listener.onAudio(audioBytes)));
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
		if (session != null) {
			try {
				LiveSendRealtimeInputParameters parameters = LiveSendRealtimeInputParameters.builder()
						                                             .media(Blob.builder().mimeType("audio/pcm;rate=16000").data(audioData).build())
						                                             .build();
				session.sendRealtimeInput(parameters);
			} catch (Exception e) {
				logger.error("Error sending audio to Gemini: {}", e.getMessage());
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
