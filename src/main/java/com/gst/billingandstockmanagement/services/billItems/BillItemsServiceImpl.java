package com.gst.billingandstockmanagement.services.billItems;

import java.util.Optional;

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
        // Fetch the Bill entity from the database
        Bill bill = billRepository.findById(billItemsDTO.getBillId()).orElse(null);

        BillItems billItems;
        boolean isUpdate = false;

        if (billItemsDTO.getId() != null) {
            // Attempt update
            Optional<BillItems> existingOpt = billItemsRepository.findById(billItemsDTO.getId());
            if (existingOpt.isPresent()) {
                billItems = existingOpt.get();
                isUpdate = true;
            } else {
                billItems = new BillItems();
                billItems.setId(billItemsDTO.getId());
            }
        } else {
            billItems = new BillItems();
        }

        billItems.setBill(bill);

        // If a productId is provided, fetch it and set relationship
        Product product = null;
        if (billItemsDTO.getProductId() != null) {
            product = productRepository.findById(billItemsDTO.getProductId()).orElse(null);
            billItems.setProduct(product);
        }

        // If updating, enforce stricter validation: if DTO provides snapshot name/hsn, they must match the current product
        if (isUpdate && product != null) {
            if (billItemsDTO.getSnapshotProductName() != null && !billItemsDTO.getSnapshotProductName().equals(product.getName())) {
                throw new IllegalArgumentException("Snapshot product name does not match the selected product's current name");
            }
            if (billItemsDTO.getSnapshotHsn() != null && !billItemsDTO.getSnapshotHsn().equals(product.getHSN())) {
                throw new IllegalArgumentException("Snapshot HSN does not match the selected product's current HSN");
            }
        }

        // Populate snapshot fields from current Product (for creates and updates if product present)
        if (product != null) {
            billItems.setSnapshotProductName(product.getName());
            billItems.setSnapshotUnitPrice(product.getMRP());
            billItems.setSnapshotPacking(product.getPacking());
            billItems.setSnapshotHsn(product.getHSN());
            billItems.setSnapshotCgst(product.getCGST());
            billItems.setSnapshotSgst(product.getSGST());
        }

        // If request provided snapshot values (e.g., when creating or intentionally overriding), prefer them
        if (billItemsDTO.getSnapshotProductName() != null) {
            billItems.setSnapshotProductName(billItemsDTO.getSnapshotProductName());
        }
        if (billItemsDTO.getSnapshotUnitPrice() != null) {
            billItems.setSnapshotUnitPrice(billItemsDTO.getSnapshotUnitPrice());
        }
        if (billItemsDTO.getSnapshotPacking() != null) {
            billItems.setSnapshotPacking(billItemsDTO.getSnapshotPacking());
        }
        if (billItemsDTO.getSnapshotHsn() != null) {
            billItems.setSnapshotHsn(billItemsDTO.getSnapshotHsn());
        }
        if (billItemsDTO.getSnapshotCgst() != null) {
            billItems.setSnapshotCgst(billItemsDTO.getSnapshotCgst());
        }
        if (billItemsDTO.getSnapshotSgst() != null) {
            billItems.setSnapshotSgst(billItemsDTO.getSnapshotSgst());
        }

        billItems.setBatchNo(billItemsDTO.getBatchNo());
        billItems.setQuantity(billItemsDTO.getQuantity());
        billItems.setFree(billItemsDTO.getFree());
        billItems.setRate(billItemsDTO.getRate());
        billItems.setExpiryDate(billItemsDTO.getExpiryDate());

        // Compute amount if not provided: prefer snapshotUnitPrice, then rate
        Double computedUnit = billItems.getSnapshotUnitPrice();
        if (computedUnit == null) {
            computedUnit = billItems.getRate();
        }
        if (billItemsDTO.getAmount() != null) {
            billItems.setAmount(billItemsDTO.getAmount());
        } else {
            double qty = billItems.getQuantity();
            billItems.setAmount(computedUnit != null ? computedUnit * qty : 0.0);
        }

        // Save the BillItems entity using billItemsRepository
        billItemsRepository.save(billItems);

        // Update the total amount of the bill
        billService.updateTotalAmount(bill);
    }
}
