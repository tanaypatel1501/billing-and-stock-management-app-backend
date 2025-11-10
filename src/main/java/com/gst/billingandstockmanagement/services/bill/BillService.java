package com.gst.billingandstockmanagement.services.bill;

import java.util.List;

import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.entities.Bill;

public interface BillService {
    BillDTO addBill(BillDTO billDTO);

	void updateTotalAmount(Bill bill);

	Bill getBillById(Long billId);

	List<Bill> getAllBills();

	List<BillDTO> getAllBillsByUser(Long userId);

	void deleteBill(Long billId);
}
