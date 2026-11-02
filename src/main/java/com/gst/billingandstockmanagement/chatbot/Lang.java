package com.gst.billingandstockmanagement.chatbot;

/** The language the user wrote in, which is also the language we answer in. */
enum Lang {
    ENGLISH("English. Use plain English only; do not use any Hindi or Hinglish words."),
    HINGLISH("Hinglish: Hindi written in Roman letters, mixed with English words, the way people text in India. "
            + "Do not use Devanagari script."),
    HINDI("Hindi in Devanagari script. Keep product names, batch numbers, digits and ₹ amounts as they are.");

    private final String instruction;

    Lang(String instruction) { this.instruction = instruction; }

    String instruction() { return instruction; }
}