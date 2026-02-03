module io.github.kinsleykajiva.jai.connect {
    requires java.net.http;
    requires org.json;
    requires org.slf4j;
    requires webrtc.java;
    requires org.jspecify;
    requires com.google.genai;

    exports io.github.kinsleykajiva.ai.vendors.openai;
    exports io.github.kinsleykajiva.ai.vendors.openai.callbacks;
    exports io.github.kinsleykajiva.ai.vendors.openai.models;
    exports io.github.kinsleykajiva.ai.vendors.google;
}
