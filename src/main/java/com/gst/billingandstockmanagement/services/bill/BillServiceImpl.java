package com.gst.billingandstockmanagement.services.bill;

import com.gst.billingandstockmanagement.repository.PurchaserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.dto.BillItemsDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.BillItems;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.BillRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.repository.BillItemsRepository;
import com.gst.billingandstockmanagement.utils.PaginationUtils;
import com.gst.billingandstockmanagement.specifications.SpecificationBuilder;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BillServiceImpl implements BillService {

    @Autowired
    private BillRepository billRepository;
    
    @Autowired
    private BillItemsRepository billItemsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaserRepository purchaserRepository;

    @Override
    public BillDTO addBill(BillDTO billDTO) {
        // Fetch the User entity from the database using the userId
        User user = userRepository.findById(billDTO.getUserId()).orElse(null);

        // Convert BillDTO to Bill entity
        Bill bill = new Bill();
        bill.setId(billDTO.getId());
        bill.setUser(user);
        bill.setPurchaserName(billDTO.getPurchaserName());
        bill.setDl1(billDTO.getDl1());
        bill.setDl2(billDTO.getDl2());
        bill.setGstin(billDTO.getGstin());
        bill.setInvoiceDate(billDTO.getInvoiceDate());
        bill.setPaid(billDTO.isPaid());
        if (billDTO.getPurchaserId() != null) {
            purchaserRepository.findById(billDTO.getPurchaserId())
                    .ifPresent(bill::setPurchaser);
        }

        // Save the Bill entity using billRepository
        Bill savedBill = billRepository.save(bill);

        // Calculate and update the total amount of the bill
        updateTotalAmount(savedBill);
        
        BillDTO savedBillDTO = new BillDTO();
        savedBillDTO.setId(savedBill.getId());
        savedBillDTO.setUserId(savedBill.getUser().getId());
        savedBillDTO.setPurchaserName(savedBill.getPurchaserName());
        savedBillDTO.setDl1(savedBill.getDl1());
        savedBillDTO.setDl2(savedBill.getDl2());
        savedBillDTO.setGstin(savedBill.getGstin());
        savedBillDTO.setInvoiceDate(savedBill.getInvoiceDate());

        return savedBillDTO;
    }

    // Helper method to update the total amount of a bill
    public void updateTotalAmount(Bill bill) {
        Double totalAmount = billRepository.calculateTotalAmount(bill);
        if (totalAmount == null) {
            totalAmount = 0.0;
        }
        bill.setTotalAmount(totalAmount);
        billRepository.save(bill); // Update the bill with the new total amount
    }
    
    @Override
    public BillDTO getBillById(Long billId) {
        // Fetch the Bill entity by its ID
        Bill bill = billRepository.findById(billId).orElse(null);

        if (bill == null) {
            return null;
        }

        BillDTO dto = convertToBillDTO(bill);
        return dto;
    }
    
    @Override
    public List<BillDTO> getAllBills() {
        List<Bill> bills = billRepository.findAll();
        return bills.stream().map(this::convertToBillDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<BillDTO> getAllBillsByUser(Long userId) {
        // Fetch the User entity from the database using the userId
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            // Handle the case where the user is not found
            throw new RuntimeException("User not found with ID: " + userId);
        }

        // Fetch all bills associated with the user
        List<Bill> userBills = billRepository.findByUser(user);

        // Convert the list of Bill entities to a list of BillDTOs
        List<BillDTO> userBillDTOs = userBills.stream()
                .map(this::convertToBillDTO)
                .collect(Collectors.toList());

        return userBillDTOs;
    }

    // Helper method to convert a Bill entity to a BillDTO
    private BillDTO convertToBillDTO(Bill bill) {
        BillDTO billDTO = new BillDTO();
        billDTO.setId(bill.getId());
        billDTO.setUserId(bill.getUser().getId());
        billDTO.setPurchaserName(bill.getPurchaserName());
        billDTO.setDl1(bill.getDl1());
        billDTO.setDl2(bill.getDl2());
        billDTO.setGstin(bill.getGstin());
        billDTO.setInvoiceDate(bill.getInvoiceDate());
        billDTO.setTotalAmount(bill.getTotalAmount());
        billDTO.setPaid(bill.isPaid());

        // Map bill items to DTOs using snapshot fields
        if (bill.getBillItems() != null) {
            List<BillItemsDTO> items = bill.getBillItems().stream().map(item -> {
                BillItemsDTO it = new BillItemsDTO();
                it.setId(item.getId());
                it.setBillId(bill.getId());
                it.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
                it.setSnapshotProductName(item.getSnapshotProductName());
                it.setSnapshotUnitPrice(item.getSnapshotUnitPrice());
                it.setSnapshotPacking(item.getSnapshotPacking());
                it.setSnapshotHsn(item.getSnapshotHsn());
                it.setSnapshotCgst(item.getSnapshotCgst());
                it.setSnapshotSgst(item.getSnapshotSgst());
                it.setBatchNo(item.getBatchNo());
                it.setQuantity(item.getQuantity());
                it.setFree(item.getFree());
                it.setRate(item.getRate());
                it.setExpiryDate(item.getExpiryDate());
                it.setAmount(item.getAmount());
                return it;
            }).collect(Collectors.toList());

            billDTO.setBillItems(items);
        }

        return billDTO;
    }

    @Override
    public void deleteBill(Long billId) {
        // Fetch the Bill entity by its ID
        Bill bill = billRepository.findById(billId).orElse(null);

        if (bill != null) {
            // Fetch the associated BillItems
            List<BillItems> billItems = bill.getBillItems();

            // Delete each BillItem
            for (BillItems item : billItems) {
                billItemsRepository.delete(item);
            }

            // Delete the Bill
            billRepository.delete(bill);
        } else {
            throw new RuntimeException("Bill not found with ID: " + billId);
        }
    }

    @Override
    public Page<Bill> searchWithPagination(SearchRequest request) {
        SpecificationBuilder<Bill> builder = new SpecificationBuilder<>();
        List<String> fields = List.of(
                "purchaserName", "gstin",
                "billItems.snapshotProductName", "invoiceDate"
        );
        Pageable pageable = PaginationUtils.getPageable(request);

        Map<String, String> filters = request.getFilters() == null
                ? new HashMap<>()
                : new HashMap<>(request.getFilters());

        String fromDateStr = filters.remove("invoiceDate.from");
        String toDateStr   = filters.remove("invoiceDate.to");

        request.setFilters(filters);
        Specification<Bill> baseSpec = builder.build(
                request.getSearchText(), fields, request.getFilters()
        );

        Specification<Bill> dateSpec      = buildDateRangeSpec(fromDateStr, toDateStr);
        Specification<Bill> purchaserSpec = buildPurchaserSpec(request.getPurchaserId());

        Specification<Bill> finalSpec = Specification.where(baseSpec);
        if (dateSpec != null)      finalSpec = finalSpec.and(dateSpec);
        if (purchaserSpec != null) finalSpec = finalSpec.and(purchaserSpec);

        return billRepository.findAll(finalSpec, pageable);
    }

    private Specification<Bill> buildPurchaserSpec(Long purchaserId) {
        if (purchaserId == null) return null;
        return (root, query, cb) ->
                cb.equal(root.get("purchaser").get("id"), purchaserId);
    }

    private Specification<Bill> buildDateRangeSpec(String fromStr, String toStr) {
        if (fromStr == null && toStr == null) return null;

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            try {
                if (fromStr != null && !fromStr.isBlank()) {
                    Date from = sdf.parse(fromStr);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), from));
                }
                if (toStr != null && !toStr.isBlank()) {
                    // Add 1 day to make "to" inclusive of the full day
                    Date to = sdf.parse(toStr);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(to);
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
                    predicates.add(cb.lessThan(root.get("invoiceDate"), cal.getTime()));
                }
            } catch (Exception e) {
                // Bad date format — ignore silently, don't crash the search
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public void updatePaidStatus(Long billId, boolean paid) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + billId));
        bill.setPaid(paid);
        billRepository.save(bill);
    }
}
