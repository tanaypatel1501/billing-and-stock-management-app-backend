package com.gst.billingandstockmanagement.chatbot;

/** Every fixed message the bot writes itself, in all three languages. */
enum Msg {

    REFUSAL(
            "I can only help with your sales, bills, purchasers and stock in this app. Try something like “revenue this month” or “top 5 products”.",
            "Main sirf aapki sales, bills, purchasers aur stock ke baare mein madad kar sakta hoon. Aap “is mahine ka revenue” ya “top 5 products” jaisa kuch poochh sakte hain.",
            "मैं केवल आपकी बिक्री, बिल, ग्राहकों और स्टॉक के बारे में मदद कर सकता हूँ। आप “इस महीने का राजस्व” या “टॉप 5 प्रोडक्ट” जैसा कुछ पूछ सकते हैं।"),

    BUSY(
            "The assistant is busy right now. Please try again in a minute.",
            "Assistant abhi busy hai. Kripya ek minute baad dobara try karein.",
            "असिस्टेंट अभी व्यस्त है। कृपया एक मिनट बाद फिर कोशिश करें।"),

    FAILED(
            "I couldn't work that one out. Try rephrasing it, for example by naming the month or product.",
            "Main ye samajh nahi paya. Kripya dusre tareeke se poochhein, jaise mahine ya product ka naam likhkar.",
            "मैं इसे समझ नहीं पाया। कृपया दूसरे तरीके से पूछें, जैसे महीने या प्रोडक्ट का नाम लिखकर।"),

    TRY_AGAIN(
            "I couldn't work that one out. Please try again.",
            "Main ye samajh nahi paya. Kripya dobara try karein.",
            "मैं इसे समझ नहीं पाया। कृपया फिर से कोशिश करें।"),

    CLARIFY_DEFAULT(
            "Could you tell me a bit more about what you want to know?",
            "Kya aap thoda aur bata sakte hain ki aap kya jaanna chahte hain?",
            "क्या आप थोड़ा और बता सकते हैं कि आप क्या जानना चाहते हैं?"),

    NOT_AVAILABLE_DEFAULT(
            "That information isn't tracked in the app.",
            "Ye jaankari app mein record nahi hoti.",
            "यह जानकारी ऐप में दर्ज नहीं होती।"),

    // args: 1 = purchaser/product label, 2 = what the user typed
    NOT_FOUND(
            "I couldn't find a %1$s matching “%2$s”. Check the spelling or try part of the name.",
            "Mujhe “%2$s” se milta-julta koi %1$s nahi mila. Spelling check karein ya naam ka kuch hissa likhkar dekhein.",
            "“%2$s” से मिलता-जुलता कोई %1$s नहीं मिला। वर्तनी जाँचें या नाम का कुछ हिस्सा लिखकर देखें।"),

    CHOICE_PROMPT(
            "I found a few %1$ss that could match “%2$s”. Which one did you mean?",
            "“%2$s” se milte-julte kai %1$s mile hain. Aapka matlab kaun sa tha?",
            "“%2$s” से मिलते-जुलते कई %1$s मिले हैं। आपका मतलब कौन सा था?"),

    EXPIRED_CHOICE(
            "That choice has expired. Please ask your question again.",
            "Ye option expire ho gaya. Kripya apna sawaal dobara poochhein.",
            "यह विकल्प समाप्त हो गया है। कृपया अपना प्रश्न फिर से पूछें।"),

    INVALID_CHOICE(
            "That option isn't valid. Please ask your question again.",
            "Ye option sahi nahi hai. Kripya apna sawaal dobara poochhein.",
            "यह विकल्प मान्य नहीं है। कृपया अपना प्रश्न फिर से पूछें।"),

    NO_ROWS(
            "I didn't find anything matching that.",
            "Isse match karta hua kuch nahi mila.",
            "इससे मेल खाता कुछ नहीं मिला।"),

    // arg: row count
    ROWS_HEADER(
            "Here are the results (%1$d rows):",
            "Ye rahe results (%1$d rows):",
            "ये रहे परिणाम (%1$d पंक्तियाँ):");

    private final String en, hinglish, hindi;

    Msg(String en, String hinglish, String hindi) {
        this.en = en;
        this.hinglish = hinglish;
        this.hindi = hindi;
    }

    String in(Lang lang, Object... args) {
        String text = switch (lang) {
            case ENGLISH -> en;
            case HINGLISH -> hinglish;
            case HINDI -> hindi;
        };
        return args.length == 0 ? text : String.format(text, args);
    }

    /** "purchaser" / "product" as it should read inside a sentence. */
    static String kind(String kind, Lang lang) {
        boolean purchaser = "purchaser".equals(kind);
        if (lang == Lang.HINDI) return purchaser ? "खरीदार" : "प्रोडक्ट";
        return purchaser ? "purchaser" : "product";
    }
}