package com.gst.billingandstockmanagement.services.purchase;

import com.gst.billingandstockmanagement.dto.PurchaseDTO;
import com.gst.billingandstockmanagement.dto.PurchaseItemsDTO;
import com.gst.billingandstockmanagement.entities.*;
import com.gst.billingandstockmanagement.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String TAX_CGST_SGST = "CGST_SGST";
    private static final String TAX_IGST = "IGST";

    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private PurchaseItemsRepository purchaseItemsRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private StockLogRepository stockLogRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public PurchaseDTO submitPurchase(PurchaseDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ---- header validation ----
        if (dto.getSupplierId() == null) throw bad("Supplier is required");
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> bad("Supplier not found"));
        if (!supplier.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this supplier.");
        }

        String invoiceNo = dto.getSupplierInvoiceNo() == null ? "" : dto.getSupplierInvoiceNo().trim();
        if (invoiceNo.isEmpty()) throw bad("Supplier invoice number is required");

        if (dto.getInvoiceDate() == null) throw bad("Invoice date is required");
        LocalDate today = LocalDate.now(IST);
        if (dto.getInvoiceDate().isAfter(today)) throw bad("Invoice date cannot be in the future");

        String taxMode = (dto.getTaxMode() == null || dto.getTaxMode().isBlank())
                ? TAX_CGST_SGST : dto.getTaxMode().trim().toUpperCase();
        if (!taxMode.equals(TAX_CGST_SGST) && !taxMode.equals(TAX_IGST)) {
            throw bad("Tax mode must be CGST_SGST or IGST");
        }

        if (dto.getPurchaseItems() == null || dto.getPurchaseItems().isEmpty()) {
            throw bad("Add at least one item");
        }

        if (purchaseRepository.existsActiveDuplicate(user.getId(), supplier.getName(), invoiceNo)) {
            throw conflict("A purchase with invoice no. " + invoiceNo + " from " + supplier.getName() + " already exists");
        }

        // ---- create the purchase header ----
        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setSupplier(supplier);
        purchase.setSupplierName(supplier.getName());
        purchase.setSupplierGstin(supplier.getGstin());
        purchase.setSupplierInvoiceNo(invoiceNo);
        purchase.setInvoiceDate(dto.getInvoiceDate());
        purchase.setTaxMode(taxMode);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        double taxable = 0, cgst = 0, sgst = 0, igst = 0;
        List<PurchaseItems> savedItems = new ArrayList<>();

        // ---- items ----
        int line = 0;
        for (PurchaseItemsDTO item : dto.getPurchaseItems()) {
            line++;
            String where = "Item " + line + ": ";

            if (item.getProductId() == null) throw bad(where + "product is required");
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> bad(where + "product not found"));

            String batchNo = item.getBatchNo() == null ? "" : item.getBatchNo().trim();
            if (batchNo.isEmpty()) throw bad(where + "batch number is required");

            if (item.getExpiryDate() == null) throw bad(where + "expiry date is required");
            if (item.getExpiryDate().isBefore(today)) throw bad(where + "expiry date is in the past");

            if (item.getQuantity() < 1) throw bad(where + "quantity must be at least 1");
            if (item.getFreeQuantity() < 0) throw bad(where + "free quantity cannot be negative");

            if (item.getPurchaseRate() == null || item.getPurchaseRate() < 0) {
                throw bad(where + "purchase rate is required");
            }
            double discount = item.getDiscountPercent() == null ? 0.0 : item.getDiscountPercent();
            if (discount < 0 || discount > 100) throw bad(where + "discount must be between 0 and 100");

            Double mrp = item.getMrp() != null ? item.getMrp() : product.getMRP();

            // Free units are not charged: amount = qty x rate, less discount
            int totalUnits = item.getQuantity() + item.getFreeQuantity();
            double amount = round2(item.getQuantity() * item.getPurchaseRate() * (1 - discount / 100.0));
            double unitCost = amount / totalUnits; // cost per unit, excluding GST (GST is claimed as ITC)

            double cgstPct = nz(product.getCGST());
            double sgstPct = nz(product.getSGST());
            taxable += amount;
            if (taxMode.equals(TAX_IGST)) {
                igst += amount * (cgstPct + sgstPct) / 100.0;
            } else {
                cgst += amount * cgstPct / 100.0;
                sgst += amount * sgstPct / 100.0;
            }

            // ---- upsert the stock batch ----
            Stock stock = stockRepository
                    .findByUserAndProductAndBatchNoAndExpiryDateAndMrp(user, product, batchNo, item.getExpiryDate(), mrp)
                    .orElse(null);

            if (stock != null) {
                // Lock the row so a concurrent bill can't read a stale quantity
                stock = stockRepository.findByIdForUpdate(stock.getId())
                        .orElseThrow(() -> new RuntimeException("Stock not found"));
                int oldQty = stock.getQuantity();
                Double oldCost = stock.getCostRate();
                stock.setQuantity(oldQty + totalUnits);
                if (oldCost != null && oldQty > 0) {
                    // Weighted average of what is on the shelf and what just arrived
                    stock.setCostRate(round4((oldQty * oldCost + totalUnits * unitCost) / (oldQty + totalUnits)));
                } else {
                    stock.setCostRate(round4(unitCost));
                }
            } else {
                stock = new Stock();
                stock.setUser(user);
                stock.setProduct(product);
                stock.setBatchNo(batchNo);
                stock.setExpiryDate(item.getExpiryDate());
                stock.setMrp(mrp);
                stock.setQuantity(totalUnits);
                stock.setCostRate(round4(unitCost));
            }
            stock = stockRepository.save(stock);

            // ---- the purchase line ----
            PurchaseItems pi = new PurchaseItems();
            pi.setPurchase(savedPurchase);
            pi.setProduct(product);
            pi.setStock(stock);
            pi.setSnapshotProductName(product.getName());
            pi.setSnapshotPacking(product.getPacking());
            pi.setSnapshotHsn(product.getHSN());
            pi.setSnapshotCgst(product.getCGST());
            pi.setSnapshotSgst(product.getSGST());
            pi.setBatchNo(batchNo);
            pi.setExpiryDate(item.getExpiryDate());
            pi.setMrp(mrp);
            pi.setQuantity(item.getQuantity());
            pi.setFreeQuantity(item.getFreeQuantity());
            pi.setPurchaseRate(item.getPurchaseRate());
            pi.setDiscountPercent(discount);
            pi.setAmount(amount);
            savedItems.add(purchaseItemsRepository.save(pi));

            // ---- stock log ----
            StockLog log = new StockLog();
            log.setStock(stock);
            log.setAction("PURCHASED");
            log.setNotes(String.format(
                    "Purchased %d%s from %s (Invoice %s)",
                    item.getQuantity(),
                    item.getFreeQuantity() > 0 ? " + " + item.getFreeQuantity() + " free" : "",
                    HtmlUtils.htmlEscape(supplier.getName()),
                    HtmlUtils.htmlEscape(invoiceNo)
            ));
            log.setTimestamp(LocalDateTime.now());
            stockLogRepository.save(log);
        }

        // ---- totals, computed here, never trusted from the client ----
        double taxableR = round2(taxable);
        double cgstR = round2(cgst);
        double sgstR = round2(sgst);
        double igstR = round2(igst);
        savedPurchase.setTaxableAmount(taxableR);
        savedPurchase.setCgstAmount(cgstR);
        savedPurchase.setSgstAmount(sgstR);
        savedPurchase.setIgstAmount(igstR);
        savedPurchase.setTotalAmount(round2(taxableR + cgstR + sgstR + igstR));
        purchaseRepository.save(savedPurchase);

        return toDTO(savedPurchase, savedItems);
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PurchaseDTO getPurchaseById(Long purchaseId, Long userId) {
        Purchase purchase = purchaseRepository.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
        return toDTO(purchase, purchaseItemsRepository.findByPurchaseId(purchaseId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseDTO> getPurchases(Long userId, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        String s = (search == null) ? "" : search.trim();
        return purchaseRepository.search(userId, s, fromDate, toDate, pageable)
                .map(p -> toDTO(p, null));
    }

    // ------------------------------------------------------------------
    // Revert
    // ------------------------------------------------------------------
    @Override
    @Transactional
    public void revertPurchase(Long purchaseId, Long userId) {
        Purchase purchase = purchaseRepository.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));
        if (purchase.isReverted()) throw conflict("This purchase has already been reverted");

        List<PurchaseItems> items = purchaseItemsRepository.findByPurchaseId(purchaseId);

        // Pass 1: lock every affected batch and make sure the goods are still on the shelf.
        // The same batch can appear on more than one line, so add the units up first.
        Map<Long, Integer> unitsByStock = new HashMap<>();
        for (PurchaseItems item : items) {
            if (item.getStock() == null) {
                throw conflict("Cannot revert: the stock batch " + item.getBatchNo() + " no longer exists");
            }
            unitsByStock.merge(item.getStock().getId(), item.getQuantity() + item.getFreeQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> e : unitsByStock.entrySet()) {
            Stock stock = stockRepository.findByIdForUpdate(e.getKey())
                    .orElseThrow(() -> conflict("Cannot revert: a stock batch no longer exists"));
            if (stock.getQuantity() < e.getValue()) {
                throw conflict(String.format(
                        "Cannot revert: batch %s has only %d of the %d purchased units left (some were sold)",
                        stock.getBatchNo(), stock.getQuantity(), e.getValue()));
            }
        }

        // Pass 2: take the stock back out
        for (PurchaseItems item : items) {
            Stock stock = stockRepository.findByIdForUpdate(item.getStock().getId())
                    .orElseThrow(() -> conflict("Cannot revert: a stock batch no longer exists"));

            int remove = item.getQuantity() + item.getFreeQuantity();
            int oldQty = stock.getQuantity();
            int newQty = oldQty - remove;

            // Undo this line's contribution to the weighted-average cost
            Double oldCost = stock.getCostRate();
            if (newQty > 0 && oldCost != null) {
                double itemCost = item.getAmount() / remove;
                double restored = (oldQty * oldCost - remove * itemCost) / newQty;
                if (Double.isFinite(restored) && restored >= 0) {
                    stock.setCostRate(round4(restored));
                }
            }
            stock.setQuantity(newQty);
            stockRepository.save(stock);

            StockLog log = new StockLog();
            log.setStock(stock);
            log.setAction("PURCHASE_REVERTED");
            log.setNotes(String.format(
                    "Reverted purchase of %d%s from %s (Invoice %s)",
                    item.getQuantity(),
                    item.getFreeQuantity() > 0 ? " + " + item.getFreeQuantity() + " free" : "",
                    HtmlUtils.htmlEscape(purchase.getSupplierName()),
                    HtmlUtils.htmlEscape(purchase.getSupplierInvoiceNo())
            ));
            log.setTimestamp(LocalDateTime.now());
            stockLogRepository.save(log);
        }

        purchase.setReverted(true);
        purchaseRepository.save(purchase);
    }

    // ------------------------------------------------------------------
    // Mapping and helpers
    // ------------------------------------------------------------------
    private PurchaseDTO toDTO(Purchase p, List<PurchaseItems> items) {
        PurchaseDTO dto = new PurchaseDTO();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getId());
        dto.setSupplierId(p.getSupplier() != null ? p.getSupplier().getId() : null);
        dto.setSupplierName(p.getSupplierName());
        dto.setSupplierGstin(p.getSupplierGstin());
        dto.setSupplierInvoiceNo(p.getSupplierInvoiceNo());
        dto.setInvoiceDate(p.getInvoiceDate());
        dto.setTaxMode(p.getTaxMode());
        dto.setTaxableAmount(p.getTaxableAmount());
        dto.setCgstAmount(p.getCgstAmount());
        dto.setSgstAmount(p.getSgstAmount());
        dto.setIgstAmount(p.getIgstAmount());
        dto.setTotalAmount(p.getTotalAmount());
        dto.setReverted(p.isReverted());
        dto.setCreatedAt(p.getCreatedAt());
        if (items != null) {
            List<PurchaseItemsDTO> list = new ArrayList<>();
            for (PurchaseItems i : items) list.add(toItemDTO(i));
            dto.setPurchaseItems(list);
        }
        return dto;
    }

    private PurchaseItemsDTO toItemDTO(PurchaseItems i) {
        PurchaseItemsDTO dto = new PurchaseItemsDTO();
        dto.setId(i.getId());
        dto.setPurchaseId(i.getPurchase().getId());
        dto.setProductId(i.getProduct() != null ? i.getProduct().getId() : null);
        dto.setStockId(i.getStock() != null ? i.getStock().getId() : null);
        dto.setSnapshotProductName(i.getSnapshotProductName());
        dto.setSnapshotPacking(i.getSnapshotPacking());
        dto.setSnapshotHsn(i.getSnapshotHsn());
        dto.setSnapshotCgst(i.getSnapshotCgst());
        dto.setSnapshotSgst(i.getSnapshotSgst());
        dto.setBatchNo(i.getBatchNo());
        dto.setExpiryDate(i.getExpiryDate());
        dto.setMrp(i.getMrp());
        dto.setQuantity(i.getQuantity());
        dto.setFreeQuantity(i.getFreeQuantity());
        dto.setPurchaseRate(i.getPurchaseRate());
        dto.setDiscountPercent(i.getDiscountPercent());
        dto.setAmount(i.getAmount());
        return dto;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}