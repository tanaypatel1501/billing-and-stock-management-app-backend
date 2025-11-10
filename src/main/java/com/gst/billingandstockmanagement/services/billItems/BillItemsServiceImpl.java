package com.gst.billingandstockmanagement.services.billItems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gst.billingandstockmanagement.dto.BillItemsDTO;
import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.BillItems;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import com.gst.billingandstockmanagement.repository.BillRepository;
import com.gst.billingandstockmanagement.repository.ProductRepository;
import com.gst.billingandstockmanagement.services.bill.BillService;

@Service
public class BillItemsServiceImpl implements BillItemsService {

    @Autowired
    private BillItemsRepository billItemsRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BillService billService; // Inject the BillService

    @Override
    public void addBillItems(BillItemsDTO billItemsDTO) {
        // Fetch the Bill and Product entities from the database
        Bill bill = billRepository.findById(billItemsDTO.getBillId()).orElse(null);
        Product product = productRepository.findById(billItemsDTO.getProductId()).orElse(null);

        // Convert BillItemsDTO to BillItems entity
        BillItems billItems = new BillItems();
        billItems.setId(billItemsDTO.getId());
        billItems.setBill(bill);
        billItems.setProduct(product);
        billItems.setBatchNo(billItemsDTO.getBatchNo());
        billItems.setQuantity(billItemsDTO.getQuantity());
        billItems.setFree(billItemsDTO.getFree());
        billItems.setRate(billItemsDTO.getRate());
        billItems.setExpiryDate(billItemsDTO.getExpiryDate());
        billItems.setAmount(billItemsDTO.getAmount());

        // Save the BillItems entity using billItemsRepository
        billItemsRepository.save(billItems);

        // Update the total amount of the bill
        billService.updateTotalAmount(bill);
    }
}
