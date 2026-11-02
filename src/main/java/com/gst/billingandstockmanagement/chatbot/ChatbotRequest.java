package com.gst.billingandstockmanagement.chatbot;

import java.util.List;

/**
 * Normal question:   { question, history }
 * Picking an option: { token, choice }   (token came from a "needs_choice" response)
 *
 * history = the last few chat turns, sent by the browser so follow-ups like "ok show that" work.
 * It is untrusted text, exactly like the question itself (the database still enforces isolation).
 */
public record ChatbotRequest(String question, String token, String choice, List<Turn> history) {

    public record Turn(String role, String text) {}
}