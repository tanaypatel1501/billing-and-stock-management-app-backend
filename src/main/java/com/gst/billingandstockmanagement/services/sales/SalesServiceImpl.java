package com.gst.billingandstockmanagement.services.sales;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitTotalsDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import com.gst.billingandstockmanagement.services.sales.SalesService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final BillItemsRepository billItemsRepository;
    private final StockRepository stockRepository;

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

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}