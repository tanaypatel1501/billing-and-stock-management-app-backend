package com.gst.billingandstockmanagement.chatbot;

import java.util.*;

/**
 * Pure name matching (no Spring, no DB) so it is easy to unit-test.
 * "Dr. S K" matches "Dr. S.K. Jain" and "Dr. Sunil Kumar Jain": punctuation and spacing are
 * ignored, and each typed word may be a prefix of a real word. Small typos are tolerated.
 */
final class NameMatcher {

    private NameMatcher() {}

    enum Outcome { NONE, ONE, MANY }

    record Result(Outcome outcome, List<String> names) {}

    static final int MAX_OPTIONS = 5;

    static Result match(String query, Collection<String> names) {
        String q = normalize(query);
        if (q.isEmpty()) return new Result(Outcome.NONE, List.of());
        List<String> qTokens = Arrays.asList(q.split(" "));
        String qCompact = q.replace(" ", "");

        // An exact match (ignoring case/punctuation) wins outright.
        List<String> exact = new ArrayList<>();
        for (String n : names) if (normalize(n).equals(q)) exact.add(n);
        if (exact.size() == 1) return new Result(Outcome.ONE, exact);
        if (exact.size() > 1) return new Result(Outcome.MANY, top(exact));

        record Scored(String name, double score) {}
        List<Scored> scored = new ArrayList<>();

        for (String n : names) {
            String nn = normalize(n);
            List<String> nt = Arrays.asList(nn.split(" "));
            double score = 0;
            boolean all = true;

            for (String qt : qTokens) {
                double best = 0;
                for (String t : nt) {
                    if (t.equals(qt)) best = Math.max(best, 2);
                    else if (t.startsWith(qt)) best = Math.max(best, 1.5);
                    else if (qt.length() >= 4 && t.length() >= 4 && levenshtein(qt, t) <= 1) best = Math.max(best, 1);
                }
                if (best == 0) { all = false; break; }
                score += best;
            }

            if (all) {
                score -= 0.1 * Math.max(0, nt.size() - qTokens.size());   // prefer tighter matches
            } else if (qCompact.length() >= 4 && nn.replace(" ", "").contains(qCompact)) {
                all = true;                                               // "drsk" inside "drskjain"
                score = 1;
            }
            if (all) scored.add(new Scored(n, score));
        }

        scored.sort(Comparator.comparingDouble(Scored::score).reversed().thenComparing(Scored::name));
        List<String> ranked = scored.stream().map(Scored::name).toList();

        if (ranked.isEmpty()) return new Result(Outcome.NONE, List.of());
        if (ranked.size() == 1) return new Result(Outcome.ONE, ranked);
        return new Result(Outcome.MANY, top(ranked));
    }

    private static List<String> top(List<String> list) {
        return list.size() <= MAX_OPTIONS ? list : new ArrayList<>(list.subList(0, MAX_OPTIONS));
    }

    static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}