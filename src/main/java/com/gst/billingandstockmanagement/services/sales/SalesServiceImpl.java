package com.gst.billingandstockmanagement.services.sales;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitTotalsDTO;
import com.gst.billingandstockmanagement.dto.sales.GstMonthDTO;
import com.gst.billingandstockmanagement.dto.sales.GstSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.MonthlyInputGstRow;
import com.gst.billingandstockmanagement.dto.sales.MonthlyOutputGstRow;
import com.gst.billingandstockmanagement.dto.sales.OutstandingBillRow;
import com.gst.billingandstockmanagement.dto.sales.OutstandingCustomerDTO;
import com.gst.billingandstockmanagement.dto.sales.OutstandingReportDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import com.gst.billingandstockmanagement.repository.BillRepository;
import com.gst.billingandstockmanagement.repository.PurchaseRepository;
import com.gst.billingandstockmanagement.services.sales.SalesService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final BillItemsRepository billItemsRepository;
    private final StockRepository stockRepository;
    private final BillRepository billRepository;
    private final PurchaseRepository purchaseRepository;

    @Override
    public List<TopProductDTO> getTopProducts(User user, boolean paidOnly, int limit) {
        int safeLimit = (limit <= 0 || limit > 20) ? 5 : limit;
        return billItemsRepository.findTopProducts(user.getId(), paidOnly, safeLimit);
    }

    @Override
    public List<PeriodSalesDTO> getMonthlySales(User user, int year, boolean paidOnly) {
        return billItemsRepository.findMonthlySales(user.getId(), year, paidOnly);
    }

    @Override
    public List<PeriodSalesDTO> getYearlySales(User user, boolean paidOnly) {
        return billItemsRepository.findYearlySales(user.getId(), paidOnly);
    }

    @Override
    public SalesSummaryDTO getSalesSummary(User user, boolean paidOnly) {
        SalesSummaryDTO summary = billItemsRepository.findSalesSummary(user.getId(), paidOnly);
        if (summary == null) {
            return new SalesSummaryDTO(0.0, 0L, 0L);
        }
        return summary;
    }

    @Override
    public List<Integer> getAvailableYears(User user) {
        return billItemsRepository.findAvailableYears(user.getId());
    }

    @Override
    public ProfitSummaryDTO getProfitSummary(User user, boolean paidOnly, LocalDate fromDate, LocalDate toDate) {
        ProfitTotalsDTO t = billItemsRepository.findProfitTotals(user.getId(), paidOnly, fromDate, toDate);

        double totalRevenue = t == null ? 0.0 : nz(t.getTotalRevenue());
        double costedRevenue = t == null ? 0.0 : nz(t.getCostedRevenue());
        double costedCost = t == null ? 0.0 : nz(t.getCostedCost());
        double profit = costedRevenue - costedCost;

        ProfitSummaryDTO dto = new ProfitSummaryDTO();
        dto.setSalesExGst(round2(totalRevenue));
        dto.setSalesWithCost(round2(costedRevenue));
        dto.setCost(round2(costedCost));
        dto.setProfit(round2(profit));
        dto.setMarginPercent(costedRevenue > 0 ? round2(profit / costedRevenue * 100.0) : 0.0);
        dto.setCoveragePercent(totalRevenue > 0 ? round2(costedRevenue / totalRevenue * 100.0) : 0.0);
        dto.setTotalLines(t == null || t.getTotalLines() == null ? 0L : t.getTotalLines());
        dto.setCostedLines(t == null || t.getCostedLines() == null ? 0L : t.getCostedLines());
        dto.setTrackingSince(billItemsRepository.findTrackingSince(user.getId()));
        dto.setBatchesMissingCost(
                stockRepository.countMissingCost(user.getId(), LocalDate.now(ZoneId.of("Asia/Kolkata"))));
        return dto;
    }

    @Override
    public GstSummaryDTO getGstSummary(User user, int fyStartYear) {
        LocalDate from = LocalDate.of(fyStartYear, 4, 1);
        LocalDate to = LocalDate.of(fyStartYear + 1, 3, 31);

        Map<String, MonthlyOutputGstRow> output = new HashMap<>();
        for (MonthlyOutputGstRow r : billItemsRepository.findMonthlyOutputGst(user.getId(), from, to)) {
            output.put(r.getYm(), r);
        }
        Map<String, MonthlyInputGstRow> input = new HashMap<>();
        for (MonthlyInputGstRow r : purchaseRepository.findMonthlyInputGst(user.getId(), from, to)) {
            input.put(r.getYm(), r);
        }

        LocalDate firstPurchase = purchaseRepository.findFirstPurchaseDate(user.getId());
        YearMonth firstPurchaseMonth = firstPurchase == null ? null : YearMonth.from(firstPurchase);

        List<GstMonthDTO> months = new ArrayList<>();
        double totSalesTaxable = 0, totSalesGst = 0, totCgst = 0, totSgst = 0, totIgst = 0, totInput = 0, totNet = 0;
        int tracked = 0;

        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.of(fyStartYear, 4).plusMonths(i);
            String key = ym.toString();                    // "2026-04", same as the SQL DATE_FORMAT

            GstMonthDTO m = new GstMonthDTO();
            m.setMonth(key);

            MonthlyOutputGstRow o = output.get(key);
            m.setSalesTaxable(round2(o == null ? 0 : num(o.getTaxable())));
            m.setSalesGst(round2(o == null ? 0 : num(o.getGst())));
            totSalesTaxable += m.getSalesTaxable();
            totSalesGst += m.getSalesGst();

            // Input GST is only known from the month of the first recorded purchase onwards
            boolean isTracked = firstPurchaseMonth != null && !ym.isBefore(firstPurchaseMonth);
            m.setInputTracked(isTracked);
            if (isTracked) {
                MonthlyInputGstRow in = input.get(key);
                double cg = round2(in == null ? 0 : num(in.getCgst()));
                double sg = round2(in == null ? 0 : num(in.getSgst()));
                double ig = round2(in == null ? 0 : num(in.getIgst()));
                double inputGst = round2(cg + sg + ig);
                double net = round2(m.getSalesGst() - inputGst);

                m.setInputCgst(cg);
                m.setInputSgst(sg);
                m.setInputIgst(ig);
                m.setInputGst(inputGst);
                m.setNetGst(net);

                totCgst += cg; totSgst += sg; totIgst += ig; totInput += inputGst; totNet += net;
                tracked++;
            }
            months.add(m);
        }

        GstMonthDTO totals = new GstMonthDTO();
        totals.setMonth("TOTAL");
        totals.setSalesTaxable(round2(totSalesTaxable));
        totals.setSalesGst(round2(totSalesGst));
        totals.setInputTracked(tracked > 0);
        if (tracked > 0) {
            totals.setInputCgst(round2(totCgst));
            totals.setInputSgst(round2(totSgst));
            totals.setInputIgst(round2(totIgst));
            totals.setInputGst(round2(totInput));
            totals.setNetGst(round2(totNet));
        }

        GstSummaryDTO dto = new GstSummaryDTO();
        dto.setFyStartYear(fyStartYear);
        dto.setFromDate(from);
        dto.setToDate(to);
        dto.setFirstBillDate(billRepository.findFirstBillDate(user.getId()));
        dto.setFirstPurchaseDate(firstPurchase);
        dto.setMonths(months);
        dto.setTotals(totals);
        dto.setTrackedMonths(tracked);
        return dto;
    }

    @Override
    public OutstandingReportDTO getOutstandingReport(User user) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Map<String, OutstandingCustomerDTO> byCustomer = new LinkedHashMap<>();
        long openBills = 0;

        for (OutstandingBillRow r : billRepository.findOutstandingBills(user.getId())) {
            double outstanding = round2(num(r.getTotal()) - num(r.getPaid()));
            if (outstanding <= 0.005) {
                continue;
            }
            // A missing invoice date is not expected; if it happens the bill is treated as age 0
            LocalDate invoiceDate = r.getInvoiceDate() != null ? LocalDate.parse(r.getInvoiceDate()) : today;
            int days = (int) ChronoUnit.DAYS.between(invoiceDate, today);
            int bucket = days <= 30 ? 0 : days <= 60 ? 1 : days <= 90 ? 2 : 3;

            Long purchaserId = r.getPurchaserId() != null ? r.getPurchaserId().longValue() : null;
            String name = (r.getCustomerName() == null || r.getCustomerName().isBlank())
                    ? "Unknown customer" : r.getCustomerName().trim();
            String key = purchaserId != null ? "id:" + purchaserId : "name:" + name.toLowerCase(Locale.ROOT);

            OutstandingCustomerDTO c = byCustomer.computeIfAbsent(key, k -> {
                OutstandingCustomerDTO n = new OutstandingCustomerDTO();
                n.setPurchaserId(purchaserId);
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
            openBills++;
        }

        List<OutstandingCustomerDTO> customers = new ArrayList<>(byCustomer.values());
        double total = 0, b0 = 0, b1 = 0, b2 = 0, b3 = 0;
        for (OutstandingCustomerDTO c : customers) {
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
        customers.sort(Comparator.comparing(OutstandingCustomerDTO::getOutstanding).reversed());

        OutstandingReportDTO dto = new OutstandingReportDTO();
        dto.setAsOf(today);
        dto.setOpenBills(openBills);
        dto.setCustomerCount((long) customers.size());
        dto.setTotalOutstanding(round2(total));
        dto.setAge0to30(round2(b0));
        dto.setAge31to60(round2(b1));
        dto.setAge61to90(round2(b2));
        dto.setAgeOver90(round2(b3));
        dto.setCustomers(customers);
        return dto;
    }

    private static double num(Number n) {
        return n == null ? 0.0 : n.doubleValue();
    }

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}