package io.github.kinsleykajiva.ai.vendors.openai;

/**
 * Supported audio formats for OpenAI audio processing.
 * <p>
 * This enum defines the available audio format types:
 * <ul>
 * <li>{@code pcm16} - 24 kHz mono PCM (only a rate of 24000 is
 * supported)</li>
 * <li>{@code g711_ulaw} - G.711 μ-law compression</li>
 * <li>{@code g711_alaw} - G.711 A-law compression</li>
 * </ul>
 */
public enum AudioFormats {

	/**
	 * PCM (Pulse Code Modulation) format at 24 kHz mono.
	 * Only a sample rate of 24000 Hz is supported.
	 */
	PCM_24KHZ_MONO("pcm16", 24000, 2),

	/**
	 * G.711 μ-law (mu-law) compressed audio format.
	 * Commonly used in North American and Japanese telecommunications.
	 */
	PCMU("g711_ulaw", 8000, 1),

	/**
	 * G.711 A-law compressed audio format.
	 * Commonly used in European telecommunications.
	 */
	PCMA("g711_alaw", 8000, 1);

	private final String format;
	private final int sampleRate;
	private final int bytesPerSample;

	AudioFormats(String format, int sampleRate, int bytesPerSample) {
		this.format = format;
		this.sampleRate = sampleRate;
		this.bytesPerSample = bytesPerSample;
	}

	/**
	 * Returns the MIME type string for this audio format.
	 *
	 * @return the audio format MIME type
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Returns the sample rate for this audio format.
	 *
	 * @return the sample rate in Hz
	 */
	public int getSampleRate() {
		return sampleRate;
	}

	/**
	 * Returns the bytes per sample for this audio format.
	 *
	 * @return the bytes per sample
	 */
	public int getBytesPerSample() {
		return bytesPerSample;
	}
}