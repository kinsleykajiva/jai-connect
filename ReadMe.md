<h1><div align="center">
 <img alt="jai-connect" width="300px" height="auto" src="https://raw.githubusercontent.com/kinsleykajiva/jai-connect/main/assets/logo.png">
</div></h1>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kinsleykajiva/jai-connect.svg)](https://search.maven.org/artifact/io.github.kinsleykajiva/jai-connect)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java CI](https://github.com/kinsleykajiva/jai-connect/actions/workflows/maven.yml/badge.svg)](https://github.com/kinsleykajiva/jai-connect/actions)

# 🎙️ jai-connect: Java AI Connect

**jai-connect** is a Java-powered solution designed to access the most popular AI models from major vendors in a "Java way" using standard Maven workflows. Inspired by the flexibility of [Pipecat](https://github.com/pipecat-ai/pipecat), **jai-connect** brings real-time voice and multimodal AI agent capabilities to the Java ecosystem.

> **Note**: This project is currently under active development. Multimodal support is coming online, starting with OpenAI and Google.

## 🚀 What You Can Build

- **Voice Assistants** – Natural, streaming conversations with AI directly in Java.
- **Multimodal Interfaces** – Combine voice, text, and future video capabilities.
- **Enterprise Agents** – Integrate powerful AI models into your existing Java/Spring backends.
- **High-Performance Bots** – Leverage the speed and concurrency of Java for real-time processing.

## 🧠 Why jai-connect?

- **Java-First**: Built for Java developers. Integrates seamlessly into your Maven projects without needing Python bridges.
- **Lightweight**: Zero-dependency implementations for major protocols where possible. No heavy 3rd-party SDK bloat.
- **Real-Time**: Built-in support for ultra-low latency interactions using WebRTC (via `webrtc-java`).
- **Multimodal**: Designed from the ground up to support text, audio, and visual inputs/outputs.

## 🌐 Ecosystem

### 📦 Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.kinsleykajiva</groupId>
    <artifactId>jai-connect</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 🧩 Available Services

**jai-connect** provides abstract implementations for various AI services, allowing you to switch providers easily.

| Category            | Services                                                                                                           |
| ------------------- | ------------------------------------------------------------------------------------------------------------------ |
| **Speech-to-Text**  | [OpenAI (Whisper/Realtime)](src/main/java/io/github/kinsleykajiva/ai/vendors/openai)                               |
| **LLMs**            | [OpenAI](src/main/java/io/github/kinsleykajiva/ai/vendors/openai), [Google (Gemini)](src/main/java/io/github/kinsleykajiva/ai/vendors/google) |
| **Text-to-Speech**  | [OpenAI](src/main/java/io/github/kinsleykajiva/ai/vendors/openai)                                                  |
| **Transport**       | **WebRTC** (via `dev.onvoid.webrtc`), **WebSocket**                                                                |

## ⚡ Getting Started

Here is a simple example of initializing an OpenAI Realtime client for text-to-speech generation:

```java
import io.github.kinsleykajiva.ai.vendors.openai.OpenAITTSClient;
import io.github.kinsleykajiva.ai.vendors.openai.OpenAIModels;

public class Main {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        
        OpenAITTSClient ttsClient = new OpenAITTSClient("https://api.openai.com/v1/audio/speech", apiKey);
        
        // Use the TTS client to stream audio
        // ... implementation details
    }
}
```

Check out the `jai-connect-demo` module for complete working examples:
- [DemoSTTOpenAI.java](jai-connect-demo/src/main/java/io/github/kinsleykajiva/demo/DemoSTTOpenAI.java)
- [DemoTTSOpenAI.java](jai-connect-demo/src/main/java/io/github/kinsleykajiva/demo/DemoTTSOpenAI.java)

## 🤝 Contributing

We welcome contributions from the community! This is an open-source project and we'd love your help in bringing more "Java way" implementations of AI services.

- **Found a bug?** Open an issue.
- **Want to add a vendor?** Fork the repo and submit a PR.
- **Feature ideas?** Start a discussion.

## 📄 License

This project is licensed under the Apache 2.0 License.
