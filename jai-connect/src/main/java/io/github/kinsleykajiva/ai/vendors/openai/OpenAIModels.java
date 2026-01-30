package io.github.kinsleykajiva.ai.vendors.openai;

/**
 * Constants for OpenAI Audio and Realtime models.
 * Based on the latest documentation for Speech-to-Text and Realtime APIs.
 */
public final class OpenAIModels {

    private OpenAIModels() {
        // Private constructor to prevent instantiation
    }

    /**
     * The standard, versatile transcription and translation model.
     * Supports json, text, srt, verbose_json, and vtt output formats.
     */
    public static final String WHISPER_1 = "whisper-1";

    /**
     * Higher quality model snapshot for transcriptions.
     * Supports json or plain text output.
     */
    public static final String GPT_4O_TRANSCRIBE = "gpt-4o-transcribe";

    /**
     * Efficient, high-quality model snapshot for transcriptions.
     * Supports json or plain text output.
     */
    public static final String GPT_4O_MINI_TRANSCRIBE = "gpt-4o-mini-transcribe";

    /**
     * Model snapshot that produces speaker-aware transcripts (diarization).
     * Request the 'diarized_json' response format to receive speaker segments.
     * NOTE: Currently available via /v1/audio/transcriptions only;
     * not yet supported in the Realtime API.
     */
    public static final String GPT_4O_TRANSCRIBE_DIARIZE = "gpt-4o-transcribe-diarize";

    /**
     * Standard Realtime API model for conversation (speech-to-speech).
     * 
     * @deprecated Replaced by {@link #GPT_REALTIME}.
     */
    @Deprecated
    public static final String GPT_4O_REALTIME_PREVIEW = "gpt-4o-realtime-preview-2024-10-01";

    /**
     * The flagship production model for low-latency audio.
     * Replaces the older gpt-4o-realtime-preview.
     * Best for: High-quality customer service, complex voice agents, and
     * high-fidelity output.
     */
    public static final String GPT_REALTIME = "gpt-realtime";

    /**
     * A highly optimized, faster, and significantly cheaper version of the realtime
     * model.
     * Best for: High-volume apps, simple voice assistants, and cost-sensitive
     * products.
     */
    public static final String GPT_REALTIME_MINI = "gpt-realtime-mini";

    /**
     * The most advanced realtime model, integrating GPT-5's superior reasoning and
     * agentic capabilities into live audio.
     * Best for: High-stakes reasoning, advanced coding help via voice, and complex
     * task planning.
     */
    /**
     * The newest and most reliable text-to-speech model.
     * Supports prompting for accent, emotional range, tone, etc.
     */
    public static final String GPT_4O_MINI_TTS = "gpt-4o-mini-tts";

    /**
     * Text-to-speech model providing lower latency.
     */
    public static final String TTS_1 = "tts-1";

    /**
     * Text-to-speech model providing higher quality.
     */
    public static final String TTS_1_HD = "tts-1-hd";

    /**
     * Built-in voices for TTS and Realtime APIs.
     */
    public static final class Voices {
        /** Neutral and balanced (Androgynous). */
        public static final String ALLOY = "alloy";
        /** Professional and steady (Male). Recently added. */
        public static final String ASH = "ash";
        /** Expressive and rhythmic (Female). Recently added. */
        public static final String BALLAD = "ballad";
        /** Warm and approachable (Female). Recently added. Recommended. */
        public static final String CORAL = "coral";
        /** Warm and confident (Male). */
        public static final String ECHO = "echo";
        /** Conversational and versatile. Unique to standard TTS. */
        public static final String FABLE = "fable";
        /** Energetic and youthful (Female). Unique to standard TTS. */
        public static final String NOVA = "nova";
        /** Deep and authoritative (Male). Unique to standard TTS. */
        public static final String ONYX = "onyx";
        /** Gentle and calm (Female). Recently added. */
        public static final String SAGE = "sage";
        /** Clear and bright (Female). */
        public static final String SHIMMER = "shimmer";
        /** Energetic and crisp (Male). Recently added. */
        public static final String VERSE = "verse";
        /** Optimized for high quality. Recommended. */
        public static final String MARIN = "marin";
        /** Optimized for high quality. Recommended. */
        public static final String CEDAR = "cedar";

        /**
         * A list of all available voices for iteration or validation.
         */
        public static final java.util.List<String> ALL_VOICES = java.util.List.of(
                ALLOY, ASH, BALLAD, CORAL, ECHO, FABLE, NOVA, ONYX, SAGE, SHIMMER, VERSE, MARIN, CEDAR);

        /**
         * Voices specifically designed and recommended for the Realtime API.
         */
        public static final java.util.List<String> REALTIME_VOICES = java.util.List.of(
                ALLOY, ASH, BALLAD, CORAL, ECHO, SAGE, SHIMMER, VERSE);

        private Voices() {
        }
    }
}
