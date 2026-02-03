package io.github.kinsleykajiva.ai.vendors.google;

// defaullt - languageCode -en-US
public record GeminiConfig(String apiKey, String modelId,String voiceIdOrName, RealtimeMode realtimeMode,String part,String languageCode) {
}
