package com.gst.billingandstockmanagement.services.stock;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.ProductRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.services.email.EmailService;
import com.gst.billingandstockmanagement.utils.PaginationUtils;
import com.gst.billingandstockmanagement.specifications.SpecificationBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceImpl implements StockService {
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
	ProductRepository productRepository;
    
    @Autowired
    UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.client.dashboard-url:http://localhost:4200/user/dashboard}")
    private String dashboardUrl;

    @Override
    public StockDTO getStockById(Long stockId) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        StockDTO dto = new StockDTO();
        dto.setId(stock.getId());
        dto.setUserId(stock.getUser().getId());
        dto.setProductId(stock.getProduct().getId());
        dto.setBatchNo(stock.getBatchNo());
        dto.setExpiryDate(stock.getExpiryDate());
        dto.setQuantity(stock.getQuantity());
        dto.setMrp(stock.getMrp());

        return dto;
    }

    @Override
    public void addStock(StockDTO stockDTO) {
        User user = userRepository.findById(stockDTO.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(stockDTO.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if the user already has the same product in their stock
        Stock existingStock = stockRepository.findByUserAndProductAndBatchNoAndExpiryDate(user, product, stockDTO.getBatchNo(), stockDTO.getExpiryDate());

        if (existingStock != null) {

        	// Convert both dates to LocalDate for comparison
            LocalDate existingExpiryDate = existingStock.getExpiryDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate dtoExpiryDate = stockDTO.getExpiryDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            // Check if batch number and expiry date match
            if (existingStock.getBatchNo().equals(stockDTO.getBatchNo()) && existingExpiryDate.equals(dtoExpiryDate)) {
                // If the batch number and expiry date match, update the quantity
                int newQuantity = existingStock.getQuantity() + stockDTO.getQuantity();
                existingStock.setQuantity(newQuantity);
                if (stockDTO.getMrp() != null) {
                    existingStock.setMrp(stockDTO.getMrp());
                }
                stockRepository.save(existingStock);
            } else {
                // If the batch number or expiry date is different, create a new stock entry
                Stock stock = new Stock();
                stock.setUser(user);
                stock.setProduct(product);
                stock.setQuantity(stockDTO.getQuantity());
                stock.setBatchNo(stockDTO.getBatchNo());
                stock.setExpiryDate(stockDTO.getExpiryDate());
                stock.setMrp(stockDTO.getMrp());
                stockRepository.save(stock);
            }
        } else {
            // If the product does not exist in the user's stock, create a new stock entry
            Stock stock = new Stock();
            stock.setUser(user);
            stock.setProduct(product);
            stock.setQuantity(stockDTO.getQuantity());
            stock.setBatchNo(stockDTO.getBatchNo());
            stock.setExpiryDate(stockDTO.getExpiryDate());
            stock.setMrp(stockDTO.getMrp());
            stockRepository.save(stock);
        }
    }

    @Override
    public List<Stock> getStockByUser(User user) {
        return stockRepository.findByUser(user);
    }

    @Override
    public void updateStock(StockDTO stockDTO) {

        Stock stock = stockRepository.findById(stockDTO.getId())
                .orElseThrow(() -> new RuntimeException("Stock not found"));

        if (!stock.getUser().getId().equals(stockDTO.getUserId())) {
            throw new RuntimeException("Unauthorized stock update");
        }

        Product product = productRepository.findById(stockDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        stock.setProduct(product);
        stock.setBatchNo(stockDTO.getBatchNo());
        stock.setExpiryDate(stockDTO.getExpiryDate());
        if (stockDTO.getMrp() != null) {
            stock.setMrp(stockDTO.getMrp());
        }

        if (stockDTO.getQuantity() == 0) {
            stockRepository.delete(stock);
        } else {
            stock.setQuantity(stockDTO.getQuantity());
            stockRepository.save(stock);
        }
    }

    @Override
    public void deleteStock(Long stockId) {
        if (!stockRepository.existsById(stockId)) {
            throw new EntityNotFoundException("Stock not found with id: " + stockId);
        }
        stockRepository.deleteById(stockId);
    }

    @Override
    public Page<Stock> searchWithPagination(SearchRequest request) {
        SpecificationBuilder<Stock> builder = new SpecificationBuilder<>();
        List<String> fields = List.of("batchNo", "product.name", "product.HSN");
        Pageable pageable = PaginationUtils.getPageable(request);
        return stockRepository.findAll(builder.build(request.getSearchText(), fields, request.getFilters()), pageable);
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sendExpiryAlerts() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 30);
        Date thirtyDaysFromNow = cal.getTime();

        List<Stock> nearExpiry = stockRepository
                .findByExpiryDateBetweenAndLastExpiryNotificationDateIsNull(new Date(), thirtyDaysFromNow);

        List<Stock> alreadyExpired = stockRepository
                .findByExpiryDateBeforeAndExpiredNotificationDateIsNull(new Date());

        if (nearExpiry.isEmpty() && alreadyExpired.isEmpty()) return;

        Map<User, List<Stock>> nearMap = nearExpiry.stream()
                .collect(Collectors.groupingBy(Stock::getUser));

        Map<User, List<Stock>> expiredMap = alreadyExpired.stream()
                .collect(Collectors.groupingBy(Stock::getUser));

        Set<User> allUsers = new HashSet<>(nearMap.keySet());
        allUsers.addAll(expiredMap.keySet());

        allUsers.forEach(user -> {
            List<Stock> userNear = nearMap.getOrDefault(user, Collections.emptyList());
            List<Stock> userExpired = expiredMap.getOrDefault(user, Collections.emptyList());

            sendCombinedEmail(user, userNear, userExpired);

            Date now = new Date();
            userNear.forEach(s -> s.setLastExpiryNotificationDate(now));
            userExpired.forEach(s -> s.setExpiredNotificationDate(now));

            stockRepository.saveAll(userNear);
            stockRepository.saveAll(userExpired);
        });
    }

    private void sendCombinedEmail(User user, List<Stock> near, List<Stock> expired) {
        StringBuilder content = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy");

        content.append("<h2 style='margin:0 0 20px; font-size:24px; font-weight:800; color:#000;'>Inventory Status Alert</h2>");
        content.append("<p style='margin:0 0 16px; font-size:15px; color:#333; line-height:1.6;'>Hello ").append(user.getFirstname()).append(",</p>");
        content.append("<p style='margin:0 0 20px; font-size:15px; color:#333; line-height:1.6;'>We detected items in your stock that require immediate attention:</p>");

        if (!expired.isEmpty()) {
            content.append("<div style='margin-bottom:30px;'>");
            content.append("<h3 style='color:#c92a2a; font-size:16px; margin-bottom:10px;'>🚨 Already Expired</h3>");
            content.append(buildTable(expired, sdf, true));
            content.append("</div>");
        }

        if (!near.isEmpty()) {
            content.append("<div style='margin-bottom:30px;'>");
            content.append("<h3 style='color:#e67e22; font-size:16px; margin-bottom:10px;'>⚠️ Expiring Soon (Within 30 Days)</h3>");
            content.append(buildTable(near, sdf, false));
            content.append("</div>");
        }

        /* CTA Button */
        content.append("<table role='presentation' width='100%' style='margin:30px 0;'>")
                .append("<tr><td align='center'>")
                .append("<a href='").append(dashboardUrl).append("' ")
                .append("style='display:inline-block; background:linear-gradient(135deg,#48e3cc 0%,#36c5b0 100%);")
                .append("color:#000; padding:16px 40px; text-decoration:none; border-radius:12px; ")
                .append("font-weight:700; font-size:16px; box-shadow:0 4px 15px rgba(72,227,204,0.3);'>")
                .append("Manage Stock Dashboard</a>")
                .append("</td></tr></table>");

        String finalBody = wrapInTemplate(content.toString());
        emailService.sendEmail(user.getEmail(), "Stock Alert: Expired & Near Expiry Items - GST Medicose", finalBody);
    }

    private String buildTable(List<Stock> stocks, SimpleDateFormat sdf, boolean isCritical) {
        String accentColor = isCritical ? "#c92a2a" : "#e67e22";
        StringBuilder table = new StringBuilder("<table width='100%' cellspacing='0' cellpadding='0' style='border:1px solid #eee; border-radius:8px; overflow:hidden;'>")
                .append("<tr style='background-color:#f9f9f9;'>")
                .append("<th align='left' style='padding:12px; font-size:12px; color:#999; text-transform:uppercase;'>Product</th>")
                .append("<th align='left' style='padding:12px; font-size:12px; color:#999; text-transform:uppercase;'>Batch</th>")
                .append("<th align='left' style='padding:12px; font-size:12px; color:#999; text-transform:uppercase;'>Expiry</th>")
                .append("</tr>");

        for (Stock s : stocks) {
            table.append("<tr>")
                    .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:600;'>").append(s.getProduct().getName()).append("</td>")
                    .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; color:#666;'>").append(s.getBatchNo()).append("</td>")
                    .append("<td style='padding:12px; border-top:1px solid #eee; font-size:14px; font-weight:700; color:").append(accentColor).append(";'>")
                    .append(sdf.format(s.getExpiryDate())).append("</td>")
                    .append("</tr>");
        }
        table.append("</table>");
        return table.toString();
    }

    private String wrapInTemplate(String content) {
        String uniqueId = UUID.randomUUID().toString();
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm:ss a"));

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>" +
                "<body style='margin:0; padding:0; background-color:#efefef; font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Arial, sans-serif;'>" +
                "<div style='display:none; max-height:0; overflow:hidden;'>Inventory update from GST Medicose. Security ID: " + uniqueId + "</div>" +
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background-color:#f4f7f6;'>" +
                "<tr><td align='center' style='padding:40px 10px;'>" +
                "<div style='max-width:600px; width:100%; background:#ffffff; border-radius:16px; box-shadow:0 4px 20px rgba(0,0,0,0.05); border:1px solid #e1e8e5; overflow:hidden;'>" +

                /* Header */
                "<table role='presentation' width='100%' cellspacing='0' cellpadding='0'><tr>" +
                "<td bgcolor='#121212' align='center' style='padding:45px 20px;'>" +
                "<h1 style='margin:0; font-size:32px; font-weight:800; color:#ffffff;'>GST <span style='color:#48e3cc;'>Medicose</span></h1>" +
                "<p style='margin:10px 0 0; font-size:12px; color:#cccccc;'>Management System</p></td></tr></table>" +

                /* Content Area */
                "<div style='padding:40px 30px;'>" + content + "</div>" +

                /* Footer */
                "<div style='background:#fafafa; padding:40px; text-align:center; border-top:1px solid #f0f0f0;'>" +
                "<p style='margin:0 0 10px; font-size:13px; color:#666;'>Need help? Contact us at <a href='mailto:gstmedicose+support@gmail.com' style='color:#48e3cc; text-decoration:none; font-weight:600'>gstmedicose+support@gmail.com</a></p>" +
                "<p style='margin:0; font-size:12px; color:#bbb;'>Report generated on: " + formattedDateTime + " | ID: " + uniqueId.substring(0, 8) + "<br>&copy; 2026 GST Medicose. All Rights Reserved.</p>" +
                "</div></div></td></tr></table></body></html>";
    }
}