package com.gst.billingandstockmanagement.controllers;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private static final String OCR_URL = "http://localhost:8000/scan-label";
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestParam("image") MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(
                    file.getInputStream(), file.getOriginalFilename()
            ));

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> ocrResponse = restTemplate.postForObject(OCR_URL, request, Map.class);

            if (ocrResponse == null || !ocrResponse.containsKey("text")) {
                return ResponseEntity.status(502).body(Map.of("error", "OCR service returned no text"));
            }

            String rawText = (String) ocrResponse.get("text");
            Map<String, Object> parsed = parseLabel(rawText);
            return ResponseEntity.ok(parsed);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PARSER ───────────────────────────────────────────────────────────────
    private Map<String, Object> parseLabel(String raw) {
        String normalized = normalizeText(raw);
        String[] lines = Arrays.stream(normalized.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .toArray(String[]::new);

        return Map.of(
                "batchNo",    Objects.requireNonNullElse(extractBatchNo(lines),    ""),
                "expiryDate", Objects.requireNonNullElse(extractExpiryDate(lines), ""),
                "mrp",        Objects.requireNonNullElse(extractMrp(lines),        0.0)
        );
    }

    // ─── NORMALIZATION ────────────────────────────────────────────────────────
    private String normalizeText(String text) {
        String result = text
                .replaceAll("(?i)\\bBatch\\s*N[o0Oi]\\.?:?\\s*",               "BatchNo: ")
                .replaceAll("(?i)\\bB\\.?\\s*No\\.?:?\\s*",                     "BatchNo: ")
                .replaceAll("(?i)\\bLot\\s*N[o0]\\.?:?\\s*",                    "BatchNo: ")

                .replaceAll("(?i)\\bExp(iry)?\\s*Dat[ae]\\.?:?\\s*",            "ExpiryDate: ")
                .replaceAll("(?i)\\bExp\\.?\\s*Dt\\.?:?\\s*",                   "ExpiryDate: ")
                .replaceAll("(?i)\\bUse\\s*[Bb]y\\.?:?\\s*",                    "ExpiryDate: ")
                .replaceAll("(?i)\\bUse\\s*[Bb]efore\\.?:?\\s*",                "ExpiryDate: ")
                .replaceAll("(?i)\\bBest\\s*[Bb]efore\\.?:?\\s*",               "ExpiryDate: ")
                .replaceAll("(?i)\\bValid\\s*(Till|Upto)\\.?:?\\s*",            "ExpiryDate: ")

                .replaceAll("(?i)\\bMfg\\.?\\s*Dat[ae]\\.?:?\\s*",              "MfgDate: ")
                .replaceAll("(?i)\\bDate\\s*of\\s*(Mfg|Manufacture)\\.?:?\\s*", "MfgDate: ")
                .replaceAll("(?i)\\bMfd\\.?:?\\s*",                             "MfgDate: ")
                .replaceAll("(?i)\\bD\\.?O\\.?M\\.?:?\\s*",                     "MfgDate: ")

                .replaceAll("(?i)\\bM\\.?R\\.?P\\.?\\s*(Rs\\.?|\\u20B9)?\\s*:?\\s*", "MRP: ")
                .replaceAll("(?i)\\bMax\\.?\\s*Retail\\s*Price\\.?:?\\s*",      "MRP: ");

        // Unify named-month dates: "OCT.2027" | "OCT-2027" | "OCT 2027" | "OCT/27" → "OCT/2027"
        // Use Matcher.appendReplacement to avoid lambda split issues
        result = unifyNamedMonthDates(result);
        result = unifyNumericDates(result);
        return result;
    }

    // "JAN.2027" / "JAN-27" / "JAN 2027" → "JAN/2027"
    private String unifyNamedMonthDates(String text) {
        Pattern p = Pattern.compile(
                "(?i)\\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[.\\-/\\s]?(\\d{2,4})\\b"
        );
        Matcher m  = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String month = m.group(1).toUpperCase();
            String yr    = m.group(2);
            if (yr.length() == 2) yr = "20" + yr;
            m.appendReplacement(sb, Matcher.quoteReplacement(month + "/" + yr));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // "10/2027" | "10-27" | "10.2027" → "10/2027"
    private String unifyNumericDates(String text) {
        Pattern p = Pattern.compile("\\b(\\d{1,2})[.\\-/](\\d{2}|\\d{4})\\b");
        Matcher m  = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int mo = Integer.parseInt(m.group(1));
            if (mo < 1 || mo > 12) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            String yr = m.group(2);
            if (yr.length() == 2) yr = "20" + yr;
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "/" + yr));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ─── BATCH NUMBER ─────────────────────────────────────────────────────────
    private String extractBatchNo(String[] lines) {
        Pattern kwPat    = Pattern.compile("^BatchNo:\\s*", Pattern.CASE_INSENSITIVE);
        Pattern batchPat = Pattern.compile("^([A-Z]{1,5}-[A-Z0-9]{2,12})", Pattern.CASE_INSENSITIVE);
        Pattern monthPat = Pattern.compile("^(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)/",
                Pattern.CASE_INSENSITIVE);
        Pattern tokenPat = Pattern.compile("^[A-Z]{1,5}-[A-Z0-9]{2,12}$", Pattern.CASE_INSENSITIVE);

        // Pass 1: keyword-guided
        for (int i = 0; i < lines.length; i++) {
            if (!kwPat.matcher(lines[i]).find()) continue;
            String after = kwPat.matcher(lines[i]).replaceFirst("").trim();
            String hit   = tryBatch(after, lines, i, batchPat);
            if (hit != null) return hit;
            for (int j = i + 1; j <= Math.min(i + 2, lines.length - 1); j++) {
                String hit2 = tryBatch(lines[j], lines, j, batchPat);
                if (hit2 != null) return hit2;
            }
        }

        // Pass 2: full-doc token scan
        for (int i = 0; i < lines.length; i++) {
            for (String token : lines[i].split("\\s+")) {
                String t = token.replaceAll("^[:./ ]+|[:./ ]+$", "");
                if (!tokenPat.matcher(t).matches()) continue;
                if (monthPat.matcher(t).find())     continue;
                String suffix = t.replaceAll("^[A-Za-z]+-", "");
                if (suffix.matches("\\d{5,}"))      continue;
                return stitch(t, lines, i).toUpperCase();
            }
        }
        return null;
    }

    private String tryBatch(String text, String[] lines, int idx, Pattern batchPat) {
        Matcher m = batchPat.matcher(text);
        if (!m.find()) return null;
        return stitch(m.group(1), lines, idx).toUpperCase();
    }

    // Stitch OCR line-wrapped batch: "TP-001" + "0326" → "TP-0010326"
    private String stitch(String base, String[] lines, int idx) {
        if (idx + 1 >= lines.length) return base;
        String next = lines[idx + 1].trim();
        if (next.matches("[A-Z0-9]{2,8}") && !next.matches("\\d{4}") && !next.contains("."))
            return base + next;
        return base;
    }

    // ─── EXPIRY DATE ──────────────────────────────────────────────────────────
    private String extractExpiryDate(String[] lines) {
        Pattern expKw = Pattern.compile("^ExpiryDate:\\s*", Pattern.CASE_INSENSITIVE);
        Pattern mfgKw = Pattern.compile("^MfgDate:",        Pattern.CASE_INSENSITIVE);

        // Strategy 1: keyword proximity
        for (int i = 0; i < lines.length; i++) {
            if (!expKw.matcher(lines[i]).find()) continue;
            String inline = expKw.matcher(lines[i]).replaceFirst("").trim();
            int[]  d      = findDate(inline);
            if (d != null) return toLastDay(d);
            for (int j = i + 1; j <= Math.min(i + 2, lines.length - 1); j++) {
                int[] d2 = findDate(lines[j]);
                if (d2 != null) return toLastDay(d2);
            }
        }

        // Strategy 2: latest non-mfg date
        List<int[]> pool = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            int[] d = findDate(lines[i]);
            if (d == null) continue;
            boolean mfg = mfgKw.matcher(lines[i]).find();
            for (int off = 1; off <= 2; off++) {
                if (i - off >= 0           && mfgKw.matcher(lines[i - off]).find()) mfg = true;
                if (i + off < lines.length && mfgKw.matcher(lines[i + off]).find()) mfg = true;
            }
            pool.add(new int[]{d[0], d[1], mfg ? 1 : 0});
        }

        if (pool.isEmpty()) return null;
        boolean hasNonMfg = pool.stream().anyMatch(e -> e[2] == 0);
        List<int[]> candidates = hasNonMfg
                ? pool.stream().filter(e -> e[2] == 0).collect(Collectors.toList())
                : pool;

        candidates.sort(Comparator.comparingInt((int[] e) -> e[0]).thenComparingInt(e -> e[1]));
        return toLastDay(candidates.get(candidates.size() - 1));
    }

    // ─── MRP ──────────────────────────────────────────────────────────────────
    private Double extractMrp(String[] lines) {
        Pattern mrpKw = Pattern.compile("^MRP:\\s*", Pattern.CASE_INSENSITIVE);
        Pattern uspKw = Pattern.compile(
                "\\bUSP\\b|\\bper\\s*(tablet|cap|strip|unit|ml|gm)\\b",
                Pattern.CASE_INSENSITIVE);

        List<Double> candidates = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (!mrpKw.matcher(lines[i]).find()) continue;
            if (uspKw.matcher(lines[i]).find())  continue;
            candidates.addAll(extractNumbers(mrpKw.matcher(lines[i]).replaceFirst("").trim()));
            for (int j = i + 1; j <= Math.min(i + 3, lines.length - 1); j++) {
                if (mrpKw.matcher(lines[j]).find()) break;
                if (uspKw.matcher(lines[j]).find()) continue;
                candidates.addAll(extractNumbers(lines[j]));
            }
        }

        if (candidates.isEmpty())
            Arrays.stream(lines).forEach(l -> candidates.addAll(extractNumbers(l)));

        return candidates.stream()
                .filter(n -> n >= 1 && n < 100000 && !(n >= 2000 && n <= 2099))
                .max(Double::compareTo)
                .orElse(null);
    }

    private List<Double> extractNumbers(String text) {
        List<Double> nums = new ArrayList<>();
        Matcher m = Pattern.compile("\\b(\\d{1,6}(?:\\.\\d{1,2})?)\\b").matcher(text);
        while (m.find()) {
            try { nums.add(Double.parseDouble(m.group(1))); } catch (NumberFormatException ignored) {}
        }
        return nums;
    }

    // ─── DATE HELPERS ─────────────────────────────────────────────────────────
    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEB", 2),  Map.entry("MAR", 3),
            Map.entry("APR", 4), Map.entry("MAY", 5),  Map.entry("JUN", 6),
            Map.entry("JUL", 7), Map.entry("AUG", 8),  Map.entry("SEP", 9),
            Map.entry("OCT", 10),Map.entry("NOV", 11), Map.entry("DEC", 12)
    );

    private int[] findDate(String text) {
        // Named month: JAN/2027
        Matcher nm = Pattern.compile(
                "\\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[/\\-.\\s](20\\d{2})\\b",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (nm.find()) {
            int month = MONTH_MAP.get(nm.group(1).toUpperCase());
            int year  = Integer.parseInt(nm.group(2));
            if (year >= 2020 && year <= 2040) return new int[]{year, month};
        }

        // Numeric month: 10/2027
        Matcher dm = Pattern.compile("\\b(\\d{1,2})[/\\-.](20\\d{2})\\b").matcher(text);
        if (dm.find()) {
            int month = Integer.parseInt(dm.group(1));
            int year  = Integer.parseInt(dm.group(2));
            if (month >= 1 && month <= 12 && year >= 2020 && year <= 2040)
                return new int[]{year, month};
        }
        return null;
    }

    private String toLastDay(int[] d) {
        Calendar cal = Calendar.getInstance();
        cal.set(d[0], d[1] - 1, 1);
        int last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", d[0], d[1], last);
    }
}