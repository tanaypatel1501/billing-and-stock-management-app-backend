package com.gst.billingandstockmanagement.services.sales;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.dto.sales.ProfitSummaryDTO;
import com.gst.billingandstockmanagement.entities.User;

import java.util.List;
import java.time.LocalDate;

public interface SalesService {

    List<TopProductDTO> getTopProducts(User user, boolean paidOnly, int limit);

    List<PeriodSalesDTO> getMonthlySales(User user, int year, boolean paidOnly);

    List<PeriodSalesDTO> getYearlySales(User user, boolean paidOnly);

    SalesSummaryDTO getSalesSummary(User user, boolean paidOnly);

    List<Integer> getAvailableYears(User user);

    ProfitSummaryDTO getProfitSummary(User user, boolean paidOnly, LocalDate fromDate, LocalDate toDate);
}
