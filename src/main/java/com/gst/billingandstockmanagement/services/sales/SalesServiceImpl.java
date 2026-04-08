package com.gst.billingandstockmanagement.services.sales;

import com.gst.billingandstockmanagement.dto.sales.PeriodSalesDTO;
import com.gst.billingandstockmanagement.dto.sales.SalesSummaryDTO;
import com.gst.billingandstockmanagement.dto.sales.TopProductDTO;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import com.gst.billingandstockmanagement.services.sales.SalesService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final BillItemsRepository billItemsRepository;

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
}