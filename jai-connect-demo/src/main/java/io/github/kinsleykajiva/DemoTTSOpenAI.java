package io.github.kinsleykajiva;

import io.github.kinsleykajiva.ai.vendors.openai.OpenAIConfig;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAITTSClient;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAIModels;
import io.github.kinsleykajiva.ai.vendors.openai.AudioFormats;
import io.github.kinsleykajiva.ai.vendors.openai.RealtimeMode;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Demo for OpenAI Text-to-Speech (TTS).
 */
public class DemoTTSOpenAI {

    static void main() {
        // --- CONFIGURATION ---
        String apiKey = "";
        // Replace with your API key if not set in environment
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Please set the OPENAI_API_KEY environment variable.");
            return;
        }

        OpenAIConfig config = new OpenAIConfig(
                "https://api.openai.com/v1/realtime",
                apiKey,
                AudioFormats.PCM_24KHZ_MONO,
                AudioFormats.PCM_24KHZ_MONO,
                RealtimeMode.CONVERSATION,
                OpenAIModels.WHISPER_1);

        OpenAITTSClient ttsClient = new OpenAITTSClient(config);

        System.out.println("--- OpenAI Text-to-Speech Demo ---");
        System.out.println("Enter the text you want to convert to speech (or press Enter for default):");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        if (input.isEmpty()) {
            input = "Hello! This is a test of the new OpenAI Text-to-Speech implementation. I hope you find it useful!";
        }

        System.out.println("Generating speech using model: " + OpenAIModels.GPT_4O_MINI_TTS + " and voice: "
                + OpenAIModels.Voices.CORAL);

        try {
            // Using null for targetPath to demonstrate the random filename in the current
            // directory
            Path savedFile = ttsClient.generateSpeechToFile(
                    input,
                    OpenAIModels.Voices.CORAL,
                    OpenAIModels.GPT_4O_MINI_TTS,
                    null).join();

            System.out.println("Success! Spoken audio saved to: " + savedFile.toAbsolutePath());
            System.out.println("You can find the file in your project root.");

        } catch (Exception e) {
            System.err.println("Error generating speech: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
