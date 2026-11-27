package com.gst.billingandstockmanagement.services.sales;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gst.billingandstockmanagement.dto.sales.OutstandingCustomerDTO;
import com.gst.billingandstockmanagement.dto.sales.OutstandingReportDTO;
import com.gst.billingandstockmanagement.dto.sales.OutstandingRow;

/**
 * Turns the open invoices of one user into an aging report.
 * Shared by receivables (bills, grouped by customer) and payables (purchases, grouped by supplier).
 */
public final class OutstandingReportBuilder {

    private OutstandingReportBuilder() {
    }

    public static OutstandingReportDTO build(List<OutstandingRow> rows, String unknownPartyName) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Map<String, OutstandingCustomerDTO> byParty = new LinkedHashMap<>();
        long openCount = 0;

        for (OutstandingRow r : rows) {
            double outstanding = round2(num(r.getTotal()) - num(r.getPaid()));
            if (outstanding <= 0.005) {
                continue;
            }
            // A missing invoice date is not expected; if it happens the invoice is treated as age 0
            LocalDate invoiceDate = r.getInvoiceDate() != null ? LocalDate.parse(r.getInvoiceDate()) : today;
            int days = (int) ChronoUnit.DAYS.between(invoiceDate, today);
            int bucket = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;

            Long partyId = r.getPartyId() != null ? r.getPartyId().longValue() : null;
            String name = (r.getPartyName() == null || r.getPartyName().isBlank())
                    ? unknownPartyName : r.getPartyName().trim();
            String key = partyId != null ? "id:" + partyId : "name:" + name.toLowerCase(Locale.ROOT);

            OutstandingCustomerDTO c = byParty.computeIfAbsent(key, k -> {
                OutstandingCustomerDTO n = new OutstandingCustomerDTO();
                n.setPartyId(partyId);
                n.setName(name);
                n.setBillCount(0L);
                n.setOutstanding(0.0);
                n.setAge0to30(0.0);
                n.setAge31to60(0.0);
                n.setAge61to90(0.0);
                n.setAgeOver90(0.0);
                return n;
            });

            c.setBillCount(c.getBillCount() + 1);
            c.setOutstanding(c.getOutstanding() + outstanding);
            switch (bucket) {
                case 0 -> c.setAge0to30(c.getAge0to30() + outstanding);
                case 1 -> c.setAge31to60(c.getAge31to60() + outstanding);
                case 2 -> c.setAge61to90(c.getAge61to90() + outstanding);
                default -> c.setAgeOver90(c.getAgeOver90() + outstanding);
            }
            if (c.getOldestInvoiceDate() == null || invoiceDate.isBefore(c.getOldestInvoiceDate())) {
                c.setOldestInvoiceDate(invoiceDate);
                c.setOldestAgeDays(days);
            }
            openCount++;
        }

        List<OutstandingCustomerDTO> parties = new ArrayList<>(byParty.values());
        double total = 0, b0 = 0, b1 = 0, b2 = 0, b3 = 0;
        for (OutstandingCustomerDTO c : parties) {
            total += c.getOutstanding();
            b0 += c.getAge0to30();
            b1 += c.getAge31to60();
            b2 += c.getAge61to90();
            b3 += c.getAgeOver90();
            c.setOutstanding(round2(c.getOutstanding()));
            c.setAge0to30(round2(c.getAge0to30()));
            c.setAge31to60(round2(c.getAge31to60()));
            c.setAge61to90(round2(c.getAge61to90()));
            c.setAgeOver90(round2(c.getAgeOver90()));
        }
        parties.sort(Comparator.comparing(OutstandingCustomerDTO::getOutstanding).reversed());

        OutstandingReportDTO dto = new OutstandingReportDTO();
        dto.setAsOf(today);
        dto.setOpenBills(openCount);
        dto.setCustomerCount((long) parties.size());
        dto.setTotalOutstanding(round2(total));
        dto.setAge0to30(round2(b0));
        dto.setAge31to60(round2(b1));
        dto.setAge61to90(round2(b2));
        dto.setAgeOver90(round2(b3));
        dto.setCustomers(parties);
        return dto;
    }

    private static double num(Number n) {
        return n == null ? 0.0 : n.doubleValue();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}