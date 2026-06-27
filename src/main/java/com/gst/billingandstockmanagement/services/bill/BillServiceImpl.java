package com.gst.billingandstockmanagement.services.bill;

import com.gst.billingandstockmanagement.entities.*;
import com.gst.billingandstockmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.dto.BillItemsDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
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

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockLogRepository stockLogRepository;

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

    @Override
    @org.springframework.transaction.annotation.Transactional
    public BillDTO submitBillWithItems(BillDTO billDTO) {
        User user = userRepository.findById(billDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + billDTO.getUserId()));

        Bill bill = new Bill();
        bill.setUser(user);
        bill.setPurchaserName(billDTO.getPurchaserName());
        bill.setDl1(billDTO.getDl1());
        bill.setDl2(billDTO.getDl2());
        bill.setGstin(billDTO.getGstin());
        bill.setInvoiceDate(billDTO.getInvoiceDate());
        bill.setPaid(false);
        if (billDTO.getPurchaserId() != null) {
            purchaserRepository.findById(billDTO.getPurchaserId()).ifPresent(bill::setPurchaser);
        }
        Bill savedBill = billRepository.save(bill);

        double totalAmount = 0.0;

        if (billDTO.getBillItems() != null) {
            for (BillItemsDTO itemDTO : billDTO.getBillItems()) {
                // Locks the row for the rest of this transaction — prevents two concurrent
                // bills from both reading stale quantity and overselling the same batch
                Stock stock = stockRepository.findByIdForUpdate(itemDTO.getStockId())
                        .orElseThrow(() -> new RuntimeException("Stock not found: " + itemDTO.getStockId()));

                int totalSold = itemDTO.getQuantity() + itemDTO.getFree();
                if (totalSold > stock.getQuantity()) {
                    throw new RuntimeException(
                            "Insufficient stock for batch " + stock.getBatchNo() +
                                    " (product: " + stock.getProduct().getName() + ")"
                    );
                }

                Product product = stock.getProduct();

                BillItems billItem = new BillItems();
                billItem.setBill(savedBill);
                billItem.setProduct(product);
                billItem.setSnapshotProductName(product.getName());
                billItem.setSnapshotPacking(product.getPacking());
                billItem.setSnapshotHsn(product.getHSN());
                billItem.setSnapshotCgst(product.getCGST());
                billItem.setSnapshotSgst(product.getSGST());
                billItem.setSnapshotUnitPrice(stock.getMrp() != null ? stock.getMrp() : product.getMRP());
                billItem.setBatchNo(stock.getBatchNo());
                billItem.setExpiryDate(stock.getExpiryDate());   // copied directly — no string round-trip
                billItem.setQuantity(itemDTO.getQuantity());
                billItem.setFree(itemDTO.getFree());
                billItem.setRate(itemDTO.getRate());
                billItem.setAmount(itemDTO.getAmount());
                billItemsRepository.save(billItem);

                stock.setQuantity(stock.getQuantity() - totalSold);
                stockRepository.save(stock);

                StockLog log = new StockLog();
                log.setStock(stock);
                log.setAction("SOLD");
                log.setNotes(String.format(
                        "Sold %d%s quantity to %s via <a class=\"bill-link\" data-bill-id=\"%d\">Bill #%d</a>",
                        itemDTO.getQuantity(),
                        itemDTO.getFree() > 0 ? " + " + itemDTO.getFree() + " free" : "",
                        savedBill.getPurchaserName(),
                        savedBill.getId(), savedBill.getId()
                ));
                log.setTimestamp(java.time.LocalDateTime.now());
                stockLogRepository.save(log);

                totalAmount += itemDTO.getAmount() != null ? itemDTO.getAmount() : 0.0;
            }
        }

        savedBill.setTotalAmount(totalAmount);
        billRepository.save(savedBill);

        BillDTO result = new BillDTO();
        result.setId(savedBill.getId());
        result.setUserId(user.getId());
        result.setPurchaserName(savedBill.getPurchaserName());
        result.setTotalAmount(savedBill.getTotalAmount());
        return result;
    }
}
