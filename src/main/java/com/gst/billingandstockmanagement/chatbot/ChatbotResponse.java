package com.gst.billingandstockmanagement.chatbot;

import java.util.List;

/**
 * status: answered | refused | clarification_needed | needs_choice | not_found | failed
 * columns/rows: present when the result has more than one row (rendered as a table)
 * options/token: present for needs_choice (the user picks one, then the UI sends token + choice)
 */
public record ChatbotResponse(String status,
                              String answer,
                              String sql,
                              List<String> columns,
                              List<List<Object>> rows,
                              List<String> options,
                              String token) {

    public static ChatbotResponse text(String status, String answer) {
        return new ChatbotResponse(status, answer, null, null, null, null, null);
    }
}