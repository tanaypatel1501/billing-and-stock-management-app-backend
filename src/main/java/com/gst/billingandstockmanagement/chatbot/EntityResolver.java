package com.gst.billingandstockmanagement.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Turns what the user typed ("Dr. S K") into real names from THEIR OWN data.
 * The lookup SQL below is fixed (never model-written) and runs through the same
 * read-only, per-user views as everything else.
 */
@Component
@RequiredArgsConstructor
public class EntityResolver {

    private static final int MAX_NAMES = 3000;

    private static final String PURCHASER_NAMES =
            "SELECT DISTINCT purchaser_name AS name FROM v_bills "
                    + "WHERE purchaser_name IS NOT NULL AND purchaser_name <> ''";

    private static final String PRODUCT_NAMES =
            "SELECT DISTINCT product_name AS name FROM v_bill_items WHERE product_name IS NOT NULL "
                    + "UNION "
                    + "SELECT DISTINCT product_name AS name FROM v_stock WHERE product_name IS NOT NULL";

    private final ChatbotSqlExecutor executor;

    public NameMatcher.Result resolve(Long userId, String kind, String typedText) {
        String sql = "purchaser".equals(kind) ? PURCHASER_NAMES : PRODUCT_NAMES;
        List<String> names = executor.run(userId, sql, List.of(), MAX_NAMES).stream()
                .map(r -> r.get("name"))
                .filter(v -> v != null)
                .map(String::valueOf)
                .toList();
        return NameMatcher.match(typedText, names);
    }
}