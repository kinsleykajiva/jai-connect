package io.github.kinsleykajiva;

import io.github.kinsleykajiva.ai.vendors.openai.AudioFormats;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAIConfig;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAIRealtimeClient;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAIModels;
import io.github.kinsleykajiva.ai.vendors.openai.RealtimeMode;
import io.github.kinsleykajiva.ai.vendors.openai.callbacks.TranscriptionEventHandler;

import javax.sound.sampled.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A demo class showing how to use OpenAI Realtime API for Speech-To-Text (STT)
 * using the device's microphone.
 */
public class DemoSTTOpenAI {

	/**
	 * Helper flag to control the main loop.
	 */
	private static final AtomicBoolean running = new AtomicBoolean(true);

	static String OPENAI_API_KEY = "";

	static void main() {
		// --- CONFIGURATION ---
		String apiKey = System.getenv("OPENAI_API_KEY");
		if (apiKey == null || apiKey.isEmpty()) {
			apiKey = OPENAI_API_KEY;
		}

		if (apiKey == null || apiKey.isEmpty()) {
			System.err.println("Please set the OPENAI_API_KEY environment variable.");
			return;
		}

		String wsUrl = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01";

		OpenAIConfig config = new OpenAIConfig(
				wsUrl,
				apiKey,
				AudioFormats.PCM_24KHZ_MONO,
				AudioFormats.PCM_24KHZ_MONO,
				RealtimeMode.TRANSCRIPTION,
				OpenAIModels.WHISPER_1);

		final TargetDataLine[] lineRef = new TargetDataLine[1];
		final OpenAIRealtimeClient[] clientRef = new OpenAIRealtimeClient[1];

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\n>>> Shutdown signal received. Cleaning up...");
			running.set(false);

			// Close line to unblock the read() call in the main loop
			if (lineRef[0] != null && lineRef[0].isOpen()) {
				lineRef[0].close();
			}

			// Client cleanup
			if (clientRef[0] != null && !clientRef[0].isClosed()) {
				clientRef[0].close();
			}
			System.out.println("Shutdown complete.");
		}));

		try {
			// 1. Initialize Client
			OpenAIRealtimeClient client = new OpenAIRealtimeClient(config);
			clientRef[0] = client;

			client.registerTranscriptionHandler(new TranscriptionEventHandler() {
				@Override
				public void onTranscriptionDelta(String itemId, int contentIndex, String delta) {
					System.out.print(delta);
				}

				@Override
				public void onTranscription(String itemId, int contentIndex, String transcript) {
					System.out.println("\n[Completed]: " + transcript);
				}
			});

			System.out.println("Connecting to OpenAI Realtime API...");
			client.connect().join();
			System.out.println("Connected!");

			// 2. Initialize Microphone
			AudioFormat format = new AudioFormat(24000.0f, 16, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				System.err.println("Microphone not supported with format: " + format);
				return;
			}

			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			lineRef[0] = line;

			line.open(format);
			line.start();
			System.out.println("Listening to microphone...");
			System.out.println(">>> Start speaking now. Press Ctrl+C to stop. <<<");

			byte[] buffer = new byte[4800]; // 100ms chunks

			while (running.get()) {
				try {
					// This blocks until data is available or line is closed
					int bytesRead = line.read(buffer, 0, buffer.length);

					if (bytesRead > 0 && running.get()) {
						// Calculate simple RMS volume for debugging
						double sum = 0;
						for (int i = 0; i < bytesRead; i += 2) {
							if (i + 1 < bytesRead) {
								short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
								sum += sample * sample;
							}
						}
						double rms = Math.sqrt(sum / (bytesRead / 2.0));
						// System.out.printf("[DEBUG] Read %d bytes, Volume: %.2f%n", bytesRead, rms);

						// Only encode the actual bytes read, not the entire buffer
						byte[] actualData = new byte[bytesRead];
						System.arraycopy(buffer, 0, actualData, 0, bytesRead);
						String base64Audio = Base64.getEncoder().encodeToString(actualData);
						client.getAudioBufferManager().appendAudio(base64Audio);
					} else if (bytesRead == 0) {
						System.out.println("[DEBUG] Read 0 bytes from microphone");
					} else if (bytesRead == -1) {
						// Line closed or issues
						break;
					}
				} catch (Exception e) {
					// When line.close() is called from hook, read() might throw exception
					// If we are shutting down, this is expected
					if (running.get()) {
						System.err.println("Error reading audio: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}

		} catch (Exception e) {
			// Only print error if it wasn't during expected shutdown
			if (running.get()) {
				System.err.println("Error in demo: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}