package com.gst.billingandstockmanagement.chatbot;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides English / Hinglish / Hindi from what the user typed, in code, so the reply language does
 * not depend on the model's mood. Devanagari = Hindi. Roman-script Hindi words = Hinglish.
 */
final class LanguageDetector {

    private LanguageDetector() {}

    /** Words that are Hindi and not English, so one is enough to call it Hinglish. */
    private static final Set<String> STRONG = Set.of(
            "kya", "kitna", "kitni", "kitne", "mera", "meri", "mere", "mujhe", "hai", "hain", "tha", "thi",
            "dikhao", "dikha", "dikhaiye", "dikhana", "batao", "bata", "bataiye", "batana", "sabse", "wala",
            "wali", "wale", "mein", "aaj", "pichle", "pichhle", "kaun", "kaunsa", "kaunsi", "kab", "kyun",
            "nahi", "nahin", "abhi", "saare", "sabhi", "zyada", "becha", "bikri", "kharid", "rupaye", "kul",
            "dukaan", "maal", "baaki", "chahiye", "karo", "nikalo", "dekho", "hua", "hui", "hue", "gaya",
            "gayi", "gaye", "raha", "rahi", "rahe", "poochho", "puchho", "bhai", "yaar");

    /** Short Hindi words that could appear by accident (names etc.): two of them are needed. */
    private static final Set<String> WEAK = Set.of(
            "ka", "ki", "ke", "ko", "se", "par", "aur", "ye", "yeh", "wo", "woh", "toh", "bhi", "koi",
            "kuch", "jo", "ab", "sab", "kal", "pe", "ne");

    static Lang detect(String text, List<ChatbotRequest.Turn> history) {
        Lang own = classify(text);
        if (own != null) return own;

        // One word or no letters ("ok", "yes", "2025?"): keep the language the user was already using.
        if (history != null) {
            for (int i = history.size() - 1; i >= 0; i--) {
                ChatbotRequest.Turn t = history.get(i);
                if (t != null && "user".equals(t.role()) && t.text() != null) {
                    Lang l = classify(t.text());
                    if (l != null) return l;
                }
            }
        }
        return Lang.ENGLISH;
    }

    /** Returns null when there is not enough text to tell. */
    private static Lang classify(String text) {
        if (text == null) return null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0900' && c <= '\u097F') return Lang.HINDI;          // Devanagari
        }

        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
        int words = 0, strong = 0, weak = 0;
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            words++;
            if (STRONG.contains(t)) strong++;
            else if (WEAK.contains(t)) weak++;
        }
        if (strong >= 1 || weak >= 2) return Lang.HINGLISH;
        return words >= 2 ? Lang.ENGLISH : null;
    }
}