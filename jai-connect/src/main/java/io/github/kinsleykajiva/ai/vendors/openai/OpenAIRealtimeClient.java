package io.github.kinsleykajiva.ai.vendors.openai;

import io.github.kinsleykajiva.ai.vendors.openai.callbacks.AudioBufferManager;
import io.github.kinsleykajiva.ai.vendors.openai.callbacks.AudioEventHandler;
import io.github.kinsleykajiva.ai.vendors.openai.callbacks.SessionManager;
import io.github.kinsleykajiva.ai.vendors.openai.callbacks.TranscriptionEventHandler;
import io.github.kinsleykajiva.ai.vendors.openai.models.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* This is to be implemented where is to be used .
* *This class will support realtime communication with OpenAI services via WebSocket protocol.
* It will handle connection establishment, message sending and receiving, and error handling.
* The class will utilize the OpenAIConfig for configuration details such as API keys and endpoints.
* We will Support tts,stt and speech to speech.
* **/
public class OpenAIRealtimeClient implements WebSocket.Listener, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(OpenAIRealtimeClient.class);
	private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(10);
	private final OpenAIConfig credentialsConfig;
	private static final int BUFFER_SIZE = 64 * 1024; // 64KB buffers
	// Audio constants

	// Thread pool sizing based on system resources
	private static final int CORE_POOL_SIZE = Math.min(4, Runtime.getRuntime().availableProcessors());
	private static final int MAX_POOL_SIZE = 100; // Maximum number of buffers in pool

	private final HttpClient httpClient;
	private final ThreadPoolExecutor executor;
	private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
	private final AtomicBoolean connected = new AtomicBoolean(false);
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ScheduledExecutorService scheduler;
	// Event handling
	private final ConcurrentHashMap<String, Consumer<RealtimeEvent>> eventHandlers = new ConcurrentHashMap<>();
	private final BlockingQueue<ByteBuffer> bufferPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
	private final ConcurrentLinkedQueue<JSONObject> sendQueue = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean isSending = new AtomicBoolean(false);
	// Audio tracking
	private final AtomicLong totalAudioDurationMs = new AtomicLong(0);
	private static final String DEFAULT_INSTRUCTIONS = "You are a helpful assistant.";
	private static final Duration MIN_AUDIO_DURATION = Duration.ofMillis(100);
	private final Object durationLock = new Object();

	private static final int MAX_POOL_SIZE_THREADS = Runtime.getRuntime().availableProcessors() * 2;

	// Callback management
	private volatile Consumer<ConnectionCloseEvent> closeCallback;

	public OpenAIRealtimeClient(final OpenAIConfig credentialsConfig) {
		this.credentialsConfig = credentialsConfig;

		this.executor = new ThreadPoolExecutor(
				CORE_POOL_SIZE,
				MAX_POOL_SIZE_THREADS,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(1000),
				r -> {
					Thread t = new Thread(r, "OpenAI-Realtime-" + System.nanoTime());
					t.setDaemon(true);
					t.setUncaughtExceptionHandler(
							(thread, ex) -> logger.error("Uncaught exception in thread {}", thread.getName(), ex));
					return t;
				},
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.scheduler = Executors.newScheduledThreadPool(2, r -> {
			Thread t = new Thread(r, "OpenAI-Scheduler-" + System.nanoTime());
			t.setDaemon(true);
			return t;
		});
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(DEFAULT_CONNECTION_TIMEOUT)
				.build();
		initializeBufferPool();

	}

	private void initializeBufferPool() {
		for (int i = 0; i < MAX_POOL_SIZE; i++) {
			var resultTest = bufferPool.offer(ByteBuffer.allocateDirect(BUFFER_SIZE));
			if (!resultTest) {
				logger.warn("Failed to add buffer {} to the pool", i);
			} else {
				logger.debug("Added buffer {} to the pool", i);
			}
		}
		logger.debug("Initialized buffer pool with {} buffers of {} bytes each", MAX_POOL_SIZE, BUFFER_SIZE);
	}

	private JSONObject createSessionConfig() {
		JSONObject session = new JSONObject();

		if (this.credentialsConfig.mode() == RealtimeMode.TRANSCRIPTION) {
			session.put("input_audio_format", this.credentialsConfig.inputAudioFormat().getFormat());
			session.put("input_audio_transcription", new JSONObject()
					.put("model", this.credentialsConfig.transcriptionModel()));
			session.put("turn_detection", new JSONObject()
					.put("type", "server_vad")
					.put("threshold", 0.5)
					.put("prefix_padding_ms", 300)
					.put("silence_duration_ms", 500));
		} else {
			session.put("instructions", DEFAULT_INSTRUCTIONS)
					.put("voice", "alloy")
					.put("input_audio_format", this.credentialsConfig.inputAudioFormat().getFormat())
					.put("output_audio_format", this.credentialsConfig.outputAudioFormat().getFormat())
					.put("turn_detection", new JSONObject()
							.put("type", "server_vad")
							.put("threshold", 0.5)
							.put("prefix_padding_ms", 300)
							.put("silence_duration_ms", 200)
							.put("create_response", true));
		}
		return session;
	}

	private void initializeSession() {
		JSONObject sessionConfig = createSessionConfig();
		sendEvent(new JSONObject()
				.put("type", "session.update")
				.put("event_id", generateEventId())
				.put("session", sessionConfig));

		logger.debug("Session initialization sent");
	}

	/**
	 * Register transcription event handler
	 */
	public void registerTranscriptionHandler(TranscriptionEventHandler handler) {
		Objects.requireNonNull(handler, "Transcription handler cannot be null");
		registerEventHandler("conversation.item.input_audio_transcription.completed", event -> {
			InputAudioTranscriptionCompletedEvent transcriptionEvent = new InputAudioTranscriptionCompletedEvent(
					event.getRawEvent());
			transcriptionEvent.getItemId().ifPresent(itemId -> transcriptionEvent.getContentIndex()
					.ifPresent(contentIndex -> transcriptionEvent.getTranscript()
							.ifPresent(transcript -> handler.onTranscription(itemId, contentIndex, transcript))));
			logger.info("Audio transcription completed: {}", transcriptionEvent.getTranscript().orElse(""));
		});

		registerEventHandler("conversation.item.input_audio_transcription.delta", event -> {
			InputAudioTranscriptionDeltaEvent deltaEvent = new InputAudioTranscriptionDeltaEvent(event.getRawEvent());
			deltaEvent.getItemId().ifPresent(itemId -> deltaEvent.getContentIndex()
					.ifPresent(contentIndex -> deltaEvent.getDelta()
							.ifPresent(delta -> handler.onTranscriptionDelta(itemId, contentIndex, delta))));
		});
	}

	/**
	 * Get audio buffer manager for audio operations
	 */
	public AudioBufferManager getAudioBufferManager() {
		return new AudioBufferManagerImpl();
	}

	/**
	 * Get session manager for advanced session operations
	 */
	public SessionManager getSessionManager() {
		return new SessionManagerImpl();
	}

	/**
	 * Register audio event handler
	 */
	public void registerAudioHandler(AudioEventHandler handler) {
		Objects.requireNonNull(handler, "Audio handler cannot be null");
		registerEventHandler("response.output_audio.delta", event -> {
			ResponseOutputAudioDeltaEvent audioEvent = new ResponseOutputAudioDeltaEvent(event.getRawEvent());
			audioEvent.getItemId().ifPresent(itemId -> audioEvent.getContentIndex().ifPresent(contentIndex -> audioEvent
					.getDelta().ifPresent(base64Audio -> handler.onAudioEvent(itemId, contentIndex, base64Audio))));
		});
	}

	/**
	 * Register event handler for specific event types
	 */
	public void registerEventHandler(String eventType, Consumer<RealtimeEvent> handler) {
		Objects.requireNonNull(eventType, "Event type cannot be null");
		Objects.requireNonNull(handler, "Handler cannot be null");
		eventHandlers.put(eventType, handler);
		logger.debug("Registered handler for event type: {}", eventType);
	}

	/**
	 * Connect to OpenAI Realtime API with automatic session initialization
	 */
	public CompletableFuture<Void> connect() {
		if (closed.get()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Client is closed"));
		}

		logger.info("Connecting to OpenAI Realtime API");

		return httpClient.newWebSocketBuilder()
				.header("Authorization", "Bearer " + this.credentialsConfig.apiKey())
				.header("OpenAI-Beta", "realtime=v1")
				.buildAsync(URI.create(this.credentialsConfig.tcpResourceLink()), this)
				.thenAccept(ws -> {
					webSocketRef.set(ws);
					connected.set(true);
					logger.info("Successfully connected to OpenAI Realtime API");
					initializeSession();
				})
				.orTimeout(DEFAULT_CONNECTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
				.exceptionally(throwable -> {
					logger.error("Failed to connect to OpenAI Realtime API", throwable);
					notifyClose(new ConnectionCloseEvent(-1, "Connection failed", throwable));
					throw new RuntimeException("Connection failed", throwable);
				});
	}

	private long calculateAudioDuration(String base64Audio, AudioFormats format) {
		try {
			byte[] decodedAudio = Base64.getDecoder().decode(base64Audio);
			long samples = decodedAudio.length / format.getBytesPerSample();
			return (samples * 1000) / format.getSampleRate();
		} catch (IllegalArgumentException e) {
			logger.error("Invalid base64 audio data", e);
			return 0;
		}
	}

	private void sendEvent(JSONObject event) {
		sendQueue.offer(event);
		processSendQueue();
	}

	private void processSendQueue() {
		if (isSending.compareAndSet(false, true)) {
			sendNextEvent();
		}
	}

	private void sendNextEvent() {
		JSONObject event = sendQueue.peek();
		if (event == null) {
			isSending.set(false);
			if (!sendQueue.isEmpty()) {
				processSendQueue();
			}
			return;
		}

		WebSocket ws = webSocketRef.get();
		if (ws == null || !connected.get()) {
			logger.error("Cannot send event: WebSocket not connected");
			sendQueue.poll();
			isSending.set(false);
			return;
		}

		ByteBuffer buffer = borrowBuffer();
		try {
			String eventStr = event.toString();
			byte[] eventBytes = eventStr.getBytes();

			if (eventBytes.length > buffer.capacity()) {
				logger.error("Event too large for buffer: {} bytes", eventBytes.length);
				sendQueue.poll();
				returnBuffer(buffer);
				sendNextEvent();
				return;
			}

			sendQueue.poll();

			ws.sendText(eventStr, true)
					.handle((result, throwable) -> {
						returnBuffer(buffer);
						if (throwable != null) {
							logger.error("Failed to send WebSocket message", throwable);
						}
						sendNextEvent();
						return null;
					});

		} catch (Exception e) {
			returnBuffer(buffer);
			logger.error("Error prepairing event for send", e);
			sendQueue.poll(); // Ensure we don't get stuck on bad event
			sendNextEvent();
		}
	}

	private ByteBuffer borrowBuffer() {
		ByteBuffer buffer = bufferPool.poll();
		if (buffer == null) {
			buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
			logger.debug("Created new buffer (pool exhausted)");
		}
		return buffer;
	}

	private void returnBuffer(ByteBuffer buffer) {
		if (buffer != null) {
			buffer.clear();
			if (!bufferPool.offer(buffer)) {
				logger.debug("Buffer pool full, discarding buffer");
			}
		}
	}

	private String generateEventId() {
		return "event_" + UUID.randomUUID().toString().replace("-", "");
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		logger.info("WebSocket connection opened");
		webSocket.request(1);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		try {
			JSONObject eventJson = new JSONObject(data.toString());
			RealtimeEvent event = createEvent(eventJson);

			logger.debug("Received event: {}", event.getType());

			// 1. Internal state & logging handling
			switch (event.getType()) {
				case "error" -> handleErrorEvent((ErrorEvent) event);
				case "session.created" -> handleSessionCreated((SessionCreatedEvent) event);
				case "session.updated" -> handleSessionUpdated((SessionUpdatedEvent) event);
				case "conversation.item.added" -> handleConversationItemAdded((ConversationItemAddedEvent) event);
				case "conversation.item.done" -> handleConversationItemDone((ConversationItemDoneEvent) event);
				case "conversation.item.retrieved" ->
					handleConversationItemRetrieved((ConversationItemRetrievedEvent) event);
				case "conversation.item.input_audio_transcription.completed" ->
					handleInputAudioTranscriptionCompleted((InputAudioTranscriptionCompletedEvent) event);
				case "conversation.item.input_audio_transcription.delta" ->
					handleInputAudioTranscriptionDelta((InputAudioTranscriptionDeltaEvent) event);
				case "conversation.item.input_audio_transcription.segment" ->
					handleInputAudioTranscriptionSegment((InputAudioTranscriptionSegmentEvent) event);
				case "conversation.item.input_audio_transcription.failed" ->
					handleInputAudioTranscriptionFailed((InputAudioTranscriptionFailedEvent) event);
				case "conversation.item.truncated" ->
					handleConversationItemTruncated((ConversationItemTruncatedEvent) event);
				case "conversation.item.deleted" -> handleConversationItemDeleted((ConversationItemDeletedEvent) event);
				case "input_audio_buffer.committed" ->
					handleInputAudioBufferCommitted((InputAudioBufferCommittedEvent) event);
				case "input_audio_buffer.cleared" ->
					handleInputAudioBufferCleared((InputAudioBufferClearedEvent) event);
				case "input_audio_buffer.speech_started" ->
					handleInputAudioBufferSpeechStarted((InputAudioBufferSpeechStartedEvent) event);
				case "input_audio_buffer.speech_stopped" ->
					handleInputAudioBufferSpeechStopped((InputAudioBufferSpeechStoppedEvent) event);
				case "input_audio_buffer.timeout_triggered" ->
					handleInputAudioBufferTimeoutTriggered((InputAudioBufferTimeoutTriggeredEvent) event);
				case "response.created" -> handleResponseCreated((ResponseCreatedEvent) event);
				case "response.done" -> handleResponseDone((ResponseDoneEvent) event);
				case "response.output_item.added" ->
					handleResponseOutputItemAdded((ResponseOutputItemAddedEvent) event);
				case "response.output_item.done" -> handleResponseOutputItemDone((ResponseOutputItemDoneEvent) event);
				case "response.content_part.added" ->
					handleResponseContentPartAdded((ResponseContentPartAddedEvent) event);
				case "response.content_part.done" ->
					handleResponseContentPartDone((ResponseContentPartDoneEvent) event);
				case "response.output_text.delta" ->
					handleResponseOutputTextDelta((ResponseOutputTextDeltaEvent) event);
				case "response.output_text.done" -> handleResponseOutputTextDone((ResponseOutputTextDoneEvent) event);
				case "response.output_audio_transcript.delta" ->
					handleResponseOutputAudioTranscriptDelta((ResponseOutputAudioTranscriptDeltaEvent) event);
				case "response.output_audio_transcript.done" ->
					handleResponseOutputAudioTranscriptDone((ResponseOutputAudioTranscriptDoneEvent) event);
				case "response.output_audio.delta" ->
					handleResponseOutputAudioDelta((ResponseOutputAudioDeltaEvent) event);
				case "response.output_audio.done" ->
					handleResponseOutputAudioDone((ResponseOutputAudioDoneEvent) event);
				case "response.function_call_arguments.delta" ->
					handleResponseFunctionCallArgumentsDelta((ResponseFunctionCallArgumentsDeltaEvent) event);
				case "response.function_call_arguments.done" ->
					handleResponseFunctionCallArgumentsDone((ResponseFunctionCallArgumentsDoneEvent) event);
				case "response.mcp_call_arguments.delta" ->
					handleResponseMcpCallArgumentsDelta((ResponseMcpCallArgumentsDeltaEvent) event);
				case "response.mcp_call_arguments.done" ->
					handleResponseMcpCallArgumentsDone((ResponseMcpCallArgumentsDoneEvent) event);
				case "response.mcp_call.in_progress" ->
					handleResponseMcpCallInProgress((ResponseMcpCallInProgressEvent) event);
				case "response.mcp_call.completed" ->
					handleResponseMcpCallCompleted((ResponseMcpCallCompletedEvent) event);
				case "response.mcp_call.failed" -> handleResponseMcpCallFailed((ResponseMcpCallFailedEvent) event);
				case "mcp_list_tools.in_progress" -> handleMcpListToolsInProgress((McpListToolsInProgressEvent) event);
				case "mcp_list_tools.completed" -> handleMcpListToolsCompleted((McpListToolsCompletedEvent) event);
				case "mcp_list_tools.failed" -> handleMcpListToolsFailed((McpListToolsFailedEvent) event);
				case "rate_limits.updated" -> handleRateLimitsUpdated((RateLimitsUpdatedEvent) event);
				default -> logger.trace("Unhandled event type: {}", event.getType());
			}

			// 2. Delegate to registered handlers (always run these)
			Consumer<RealtimeEvent> handler = eventHandlers.get(event.getType());
			if (handler != null) {
				CompletableFuture.runAsync(() -> handler.accept(event), executor)
						.exceptionally(throwable -> {
							logger.error("Error in event handler for {}", event.getType(), throwable);
							return null;
						});
			}

		} catch (Exception e) {
			logger.error("Error processing event: {}", data, e);
		}

		webSocket.request(1);
		return WebSocket.Listener.super.onText(webSocket, data, last);
	}

	private RealtimeEvent createEvent(JSONObject eventJson) {
		return switch (eventJson.getString("type")) {
			case "error" -> new ErrorEvent(eventJson);
			case "session.created" -> new SessionCreatedEvent(eventJson);
			case "session.updated" -> new SessionUpdatedEvent(eventJson);
			case "conversation.item.added" -> new ConversationItemAddedEvent(eventJson);
			case "conversation.item.done" -> new ConversationItemDoneEvent(eventJson);
			case "conversation.item.retrieved" -> new ConversationItemRetrievedEvent(eventJson);
			case "conversation.item.input_audio_transcription.completed" ->
				new InputAudioTranscriptionCompletedEvent(eventJson);
			case "conversation.item.input_audio_transcription.delta" ->
				new InputAudioTranscriptionDeltaEvent(eventJson);
			case "conversation.item.input_audio_transcription.segment" ->
				new InputAudioTranscriptionSegmentEvent(eventJson);
			case "conversation.item.input_audio_transcription.failed" ->
				new InputAudioTranscriptionFailedEvent(eventJson);
			case "conversation.item.truncated" -> new ConversationItemTruncatedEvent(eventJson);
			case "conversation.item.deleted" -> new ConversationItemDeletedEvent(eventJson);
			case "input_audio_buffer.committed" -> new InputAudioBufferCommittedEvent(eventJson);
			case "input_audio_buffer.cleared" -> new InputAudioBufferClearedEvent(eventJson);
			case "input_audio_buffer.speech_started" -> new InputAudioBufferSpeechStartedEvent(eventJson);
			case "input_audio_buffer.speech_stopped" -> new InputAudioBufferSpeechStoppedEvent(eventJson);
			case "input_audio_buffer.timeout_triggered" -> new InputAudioBufferTimeoutTriggeredEvent(eventJson);
			case "response.created" -> new ResponseCreatedEvent(eventJson);
			case "response.done" -> new ResponseDoneEvent(eventJson);
			case "response.output_item.added" -> new ResponseOutputItemAddedEvent(eventJson);
			case "response.output_item.done" -> new ResponseOutputItemDoneEvent(eventJson);
			case "response.content_part.added" -> new ResponseContentPartAddedEvent(eventJson);
			case "response.content_part.done" -> new ResponseContentPartDoneEvent(eventJson);
			case "response.output_text.delta" -> new ResponseOutputTextDeltaEvent(eventJson);
			case "response.output_text.done" -> new ResponseOutputTextDoneEvent(eventJson);
			case "response.output_audio_transcript.delta" -> new ResponseOutputAudioTranscriptDeltaEvent(eventJson);
			case "response.output_audio_transcript.done" -> new ResponseOutputAudioTranscriptDoneEvent(eventJson);
			case "response.output_audio.delta" -> new ResponseOutputAudioDeltaEvent(eventJson);
			case "response.output_audio.done" -> new ResponseOutputAudioDoneEvent(eventJson);
			case "response.function_call_arguments.delta" -> new ResponseFunctionCallArgumentsDeltaEvent(eventJson);
			case "response.function_call_arguments.done" -> new ResponseFunctionCallArgumentsDoneEvent(eventJson);
			case "response.mcp_call_arguments.delta" -> new ResponseMcpCallArgumentsDeltaEvent(eventJson);
			case "response.mcp_call_arguments.done" -> new ResponseMcpCallArgumentsDoneEvent(eventJson);
			case "response.mcp_call.in_progress" -> new ResponseMcpCallInProgressEvent(eventJson);
			case "response.mcp_call.completed" -> new ResponseMcpCallCompletedEvent(eventJson);
			case "response.mcp_call.failed" -> new ResponseMcpCallFailedEvent(eventJson);
			case "mcp_list_tools.in_progress" -> new McpListToolsInProgressEvent(eventJson);
			case "mcp_list_tools.completed" -> new McpListToolsCompletedEvent(eventJson);
			case "mcp_list_tools.failed" -> new McpListToolsFailedEvent(eventJson);
			case "rate_limits.updated" -> new RateLimitsUpdatedEvent(eventJson);
			default -> new RealtimeEvent(eventJson);
		};
	}

	private void handleErrorEvent(ErrorEvent event) {
		event.getError().ifPresent(error -> {
			String message = error.optString("message", "Unknown error");
			String code = error.optString("code", "unknown");
			logger.error("Server error [{}]: {}", code, message);
		});
	}

	private void handleSessionCreated(SessionCreatedEvent event) {
		logger.info("Session created successfully");
	}

	private void handleSessionUpdated(SessionUpdatedEvent event) {
		logger.info("Session updated successfully");
	}

	private void handleConversationItemAdded(ConversationItemAddedEvent event) {
		event.getItem().ifPresent(item -> logger.debug("Conversation item added: {}", item.optString("id")));
	}

	private void handleConversationItemDone(ConversationItemDoneEvent event) {
		event.getItem().ifPresent(item -> logger.debug("Conversation item done: {}", item.optString("id")));
	}

	private void handleConversationItemRetrieved(ConversationItemRetrievedEvent event) {
		event.getItem().ifPresent(item -> logger.debug("Conversation item retrieved: {}", item.optString("id")));
	}

	private void handleInputAudioTranscriptionCompleted(InputAudioTranscriptionCompletedEvent event) {
		event.getTranscript().ifPresent(transcript -> logger.debug("Transcription completed: {}", transcript));
	}

	private void handleInputAudioTranscriptionDelta(InputAudioTranscriptionDeltaEvent event) {
		event.getDelta().ifPresent(delta -> {
			logger.debug("Transcription delta: {}", delta);
			// Registered handlers will be called by delegate in onText
		});
	}

	private void handleInputAudioTranscriptionSegment(InputAudioTranscriptionSegmentEvent event) {
		event.getText().ifPresent(text -> logger.debug("Transcription segment: {}", text));
	}

	private void handleInputAudioTranscriptionFailed(InputAudioTranscriptionFailedEvent event) {
		event.getError().ifPresent(error -> logger.error("Transcription failed: {}", error.optString("message")));
	}

	private void handleConversationItemTruncated(ConversationItemTruncatedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Item truncated: {}", itemId));
	}

	private void handleConversationItemDeleted(ConversationItemDeletedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Item deleted: {}", itemId));
	}

	private void handleInputAudioBufferCommitted(InputAudioBufferCommittedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Audio buffer committed: {}", itemId));
		synchronized (durationLock) {
			totalAudioDurationMs.set(0);
		}
	}

	private void handleInputAudioBufferCleared(InputAudioBufferClearedEvent event) {
		logger.debug("Audio buffer cleared");
		synchronized (durationLock) {
			totalAudioDurationMs.set(0);
		}
	}

	private void handleInputAudioBufferSpeechStarted(InputAudioBufferSpeechStartedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Speech started: {}", itemId));
	}

	private void handleInputAudioBufferSpeechStopped(InputAudioBufferSpeechStoppedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Speech stopped: {}", itemId));
	}

	private void handleInputAudioBufferTimeoutTriggered(InputAudioBufferTimeoutTriggeredEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Timeout triggered: {}", itemId));
	}

	private void handleResponseCreated(ResponseCreatedEvent event) {
		event.getResponse().ifPresent(response -> logger.debug("Response created: {}", response.optString("id")));
	}

	private void handleResponseDone(ResponseDoneEvent event) {
		event.getResponse().ifPresent(response -> logger.debug("Response done: {}", response.optString("id")));
	}

	private void handleResponseOutputItemAdded(ResponseOutputItemAddedEvent event) {
		event.getItem().ifPresent(item -> logger.debug("Output item added: {}", item.optString("id")));
	}

	private void handleResponseOutputItemDone(ResponseOutputItemDoneEvent event) {
		event.getItem().ifPresent(item -> logger.debug("Output item done: {}", item.optString("id")));
	}

	private void handleResponseContentPartAdded(ResponseContentPartAddedEvent event) {
		event.getPart().ifPresent(part -> logger.debug("Content part added: {}", part.optString("type")));
	}

	private void handleResponseContentPartDone(ResponseContentPartDoneEvent event) {
		event.getPart().ifPresent(part -> logger.debug("Content part done: {}", part.optString("type")));
	}

	private void handleResponseOutputTextDelta(ResponseOutputTextDeltaEvent event) {
		event.getDelta().ifPresent(delta -> logger.debug("Text delta: {}", delta));
	}

	private void handleResponseOutputTextDone(ResponseOutputTextDoneEvent event) {
		event.getText().ifPresent(text -> logger.debug("Text done: {}", text));
	}

	private void handleResponseOutputAudioTranscriptDelta(ResponseOutputAudioTranscriptDeltaEvent event) {
		event.getDelta().ifPresent(delta -> logger.debug("Audio transcript delta: {}", delta));
	}

	private void handleResponseOutputAudioTranscriptDone(ResponseOutputAudioTranscriptDoneEvent event) {
		event.getTranscript().ifPresent(transcript -> logger.debug("Audio transcript done: {}", transcript));
	}

	private void handleResponseOutputAudioDelta(ResponseOutputAudioDeltaEvent event) {
		event.getDelta().ifPresent(delta -> logger.debug("Audio delta received"));
	}

	private void handleResponseOutputAudioDone(ResponseOutputAudioDoneEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("Audio done: {}", itemId));
	}

	private void handleResponseFunctionCallArgumentsDelta(ResponseFunctionCallArgumentsDeltaEvent event) {
		event.getDelta().ifPresent(delta -> logger.debug("Function call arguments delta: {}", delta));
	}

	private void handleResponseFunctionCallArgumentsDone(ResponseFunctionCallArgumentsDoneEvent event) {
		event.getArguments().ifPresent(arguments -> logger.debug("Function call arguments done: {}", arguments));
	}

	private void handleResponseMcpCallArgumentsDelta(ResponseMcpCallArgumentsDeltaEvent event) {
		event.getDelta().ifPresent(delta -> logger.debug("MCP call arguments delta: {}", delta));
	}

	private void handleResponseMcpCallArgumentsDone(ResponseMcpCallArgumentsDoneEvent event) {
		event.getArguments().ifPresent(arguments -> logger.debug("MCP call arguments done: {}", arguments));
	}

	private void handleResponseMcpCallInProgress(ResponseMcpCallInProgressEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP call in progress: {}", itemId));
	}

	private void handleResponseMcpCallCompleted(ResponseMcpCallCompletedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP call completed: {}", itemId));
	}

	private void handleResponseMcpCallFailed(ResponseMcpCallFailedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP call failed: {}", itemId));
	}

	private void handleMcpListToolsInProgress(McpListToolsInProgressEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP list tools in progress: {}", itemId));
	}

	private void handleMcpListToolsCompleted(McpListToolsCompletedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP list tools completed: {}", itemId));
	}

	private void handleMcpListToolsFailed(McpListToolsFailedEvent event) {
		event.getItemId().ifPresent(itemId -> logger.debug("MCP list tools failed: {}", itemId));
	}

	private void handleRateLimitsUpdated(RateLimitsUpdatedEvent event) {
		event.getRateLimits().ifPresent(rateLimits -> logger.debug("Rate limits updated: {}", rateLimits.toString()));
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		connected.set(false);
		logger.info("WebSocket closed: {} - {}", statusCode, reason);
		notifyClose(new ConnectionCloseEvent(statusCode, reason));
		return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		connected.set(false);
		logger.error("WebSocket error", error);
		notifyClose(new ConnectionCloseEvent(-1, "WebSocket error", error));
	}

	private void notifyClose(ConnectionCloseEvent closeEvent) {
		Consumer<ConnectionCloseEvent> callback = closeCallback;
		if (callback != null) {
			try {
				callback.accept(closeEvent);
			} catch (Exception e) {
				logger.error("Error in close callback", e);
			}
		}
	}

	/**
	 * Set connection close callback
	 */
	public void setCloseCallback(Consumer<ConnectionCloseEvent> callback) {
		this.closeCallback = callback;
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			logger.info("Closing OpenAI Realtime client");

			WebSocket ws = webSocketRef.get();
			if (ws != null) {
				ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client closing")
						.exceptionally(throwable -> {
							logger.debug("Error during close", throwable);
							return null;
						});
			}

			connected.set(false);
			cleanup();
		}
	}

	/**
	 * Check if the client is currently connected
	 */
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Check if the client is closed
	 */
	public boolean isClosed() {
		return closed.get();
	}

	/**
	 * Get current audio buffer duration in milliseconds
	 */
	public long getCurrentAudioDurationMs() {
		return totalAudioDurationMs.get();
	}

	private void shutdownExecutor(ExecutorService executor, String name) {
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					logger.warn("Forcing shutdown of {}", name);
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				logger.warn("Interrupted while shutting down {}", name);
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	private void cleanup() {
		totalAudioDurationMs.set(0);

		// Clear buffer pool
		bufferPool.clear();

		// Shutdown executors
		shutdownExecutor(scheduler, "scheduler");
		shutdownExecutor(executor, "main executor");

		logger.info("OpenAI Realtime client cleanup completed");
	}

	private class AudioBufferManagerImpl implements AudioBufferManager {
		@Override
		public CompletableFuture<Void> appendAudio(String base64Audio) {
			Objects.requireNonNull(base64Audio, "Audio data cannot be null");

			return CompletableFuture.runAsync(() -> {
				if (!connected.get()) {
					throw new IllegalStateException("WebSocket not connected");
				}

				long durationMs = calculateAudioDuration(base64Audio, credentialsConfig.inputAudioFormat());
				JSONObject event = new JSONObject()
						.put("type", "input_audio_buffer.append")
						.put("event_id", generateEventId())
						.put("audio", base64Audio);

				logger.debug("Appending audio: {}ms, size: {} bytes", durationMs, base64Audio.length());
				sendEvent(event);

				synchronized (durationLock) {
					totalAudioDurationMs.addAndGet(durationMs);
				}
			}, executor);
		}

		@Override
		public CompletableFuture<Void> commitBuffer() {
			return CompletableFuture.runAsync(() -> {
				synchronized (durationLock) {
					long currentDuration = totalAudioDurationMs.get();
					if (currentDuration < MIN_AUDIO_DURATION.toMillis()) {
						logger.warn("Skipping buffer commit: insufficient audio duration ({}ms < {}ms required)",
								currentDuration, MIN_AUDIO_DURATION.toMillis());
						return;
					}
					JSONObject event = new JSONObject()
							.put("type", "input_audio_buffer.commit")
							.put("event_id", generateEventId());
					sendEvent(event);
					totalAudioDurationMs.set(0);
				}
			}, executor);
		}

		@Override
		public CompletableFuture<Void> clearBuffer() {
			return getSessionManager().clearAudioBuffer();
		}

		@Override
		public long getCurrentDurationMs() {
			return totalAudioDurationMs.get();
		}
	}

	private class SessionManagerImpl implements SessionManager {
		@Override
		public CompletableFuture<Void> updateInstructions(String instructions) {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "session.update")
						.put("event_id", generateEventId())
						.put("session", new JSONObject().put("instructions", instructions));
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> updateVoice(String voice) {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "session.update")
						.put("event_id", generateEventId())
						.put("session", new JSONObject().put("voice", voice));
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> createResponse(JSONObject responseConfig) {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "response.create")
						.put("event_id", generateEventId());
				if (responseConfig != null) {
					event.put("response", responseConfig);
				}
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> cancelResponse(String responseId) {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "response.cancel")
						.put("event_id", generateEventId());
				if (responseId != null) {
					event.put("response_id", responseId);
				}
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> clearAudioBuffer() {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "input_audio_buffer.clear")
						.put("event_id", generateEventId());
				sendEvent(event);
				totalAudioDurationMs.set(0);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> createConversationItem(JSONObject item, String previousItemId) {
			Objects.requireNonNull(item, "Item cannot be null");
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "conversation.item.create")
						.put("event_id", generateEventId())
						.put("item", item);
				if (previousItemId != null) {
					event.put("previous_item_id", previousItemId);
				}
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> retrieveConversationItem(String itemId) {
			Objects.requireNonNull(itemId, "Item ID cannot be null");
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "conversation.item.retrieve")
						.put("event_id", generateEventId())
						.put("item_id", itemId);
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> truncateConversationItem(String itemId, int contentIndex, int audioEndMs) {
			Objects.requireNonNull(itemId, "Item ID cannot be null");
			if (contentIndex < 0) {
				throw new IllegalArgumentException("Content index must be non-negative");
			}
			if (audioEndMs < 0) {
				throw new IllegalArgumentException("Audio end time must be non-negative");
			}
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "conversation.item.truncate")
						.put("event_id", generateEventId())
						.put("item_id", itemId)
						.put("content_index", contentIndex)
						.put("audio_end_ms", audioEndMs);
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> deleteConversationItem(String itemId) {
			Objects.requireNonNull(itemId, "Item ID cannot be null");
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "conversation.item.delete")
						.put("event_id", generateEventId())
						.put("item_id", itemId);
				sendEvent(event);
			}, executor);
		}

		@Override
		public CompletableFuture<Void> clearOutputAudioBuffer() {
			return CompletableFuture.runAsync(() -> {
				JSONObject event = new JSONObject()
						.put("type", "output_audio_buffer.clear")
						.put("event_id", generateEventId());
				sendEvent(event);
			}, executor);
		}
	}
}
