package com.gst.billingandstockmanagement.chatbot;

/** Provider-agnostic. Swap Gemini for Groq/OpenRouter/Claude by adding another implementation. */
public interface LlmClient {

    /** Returns the model's reply, which must be a JSON object (as text). */
    String completeJson(String systemPrompt, String userPrompt);

    class LlmException extends RuntimeException {
        public LlmException(String message) { super(message); }
        public LlmException(String message, Throwable cause) { super(message, cause); }
    }
}