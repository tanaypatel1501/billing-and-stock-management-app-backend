package com.gst.billingandstockmanagement.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

public class FileParser {

    public static List<Map<String, String>> parse(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename();
        if (name == null) name = "";
        String lower = name.toLowerCase();
        try (InputStream in = file.getInputStream()) {
            if (lower.endsWith(".csv") || lower.endsWith(".txt")) {
                return parseCsv(in);
            } else if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
                return parseExcel(in);
            } else {
                // fallback to CSV parser
                return parseCsv(in);
            }
        }
    }

    public static List<Map<String, String>> parseCsv(InputStream in) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            String[] headers = null;
            int rowNo = 0;
            while ((line = br.readLine()) != null) {
                String[] cols = splitCsvLine(line);
                if (rowNo == 0) {
                    headers = normalizeHeaders(cols);
                } else {
                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        String key = headers[i];
                        String val = i < cols.length ? cols[i] : "";
                        if (val == null) val = "";
                        map.put(key, val.trim());
                    }
                    rows.add(map);
                }
                rowNo++;
            }
        }
        return rows;
    }

    private static String[] normalizeHeaders(String[] cols) {
        String[] headers = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            headers[i] = cols[i] == null ? "" : cols[i].trim().toLowerCase().replaceAll("\\s+", "_");
        }
        return headers;
    }

    public static List<Map<String, String>> parseExcel(InputStream in) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();
            String[] headers = null;

            DataFormatter formatter = new DataFormatter();

            for (int r = firstRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int maxCol = row.getLastCellNum();
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < maxCol; c++) {
                    Cell cell = row.getCell(c);
                    String val = "";
                    if (cell != null) {
                        val = formatter.formatCellValue(cell);
                    }
                    cells.add(val == null ? "" : val.trim());
                }
                if (r == firstRow) {
                    headers = normalizeHeaders(cells.toArray(new String[0]));
                } else {
                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        String key = headers[i];
                        String val = i < cells.size() ? cells.get(i) : "";
                        map.put(key, val == null ? "" : val.trim());
                    }
                    rows.add(map);
                }
            }
        }
        return rows;
    }

    // Robust CSV splitter that respects quoted fields and escaped quotes
    private static String[] splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        if (line == null) return new String[0];
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // escaped quote
                    cur.append('"');
                    i++; // skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }
}
