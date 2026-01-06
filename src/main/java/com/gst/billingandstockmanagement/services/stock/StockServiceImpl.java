package com.gst.billingandstockmanagement.services.stock;

import com.gst.billingandstockmanagement.dto.StockDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.entities.Stock;
import com.gst.billingandstockmanagement.entities.User;
import com.gst.billingandstockmanagement.repository.ProductRepository;
import com.gst.billingandstockmanagement.repository.StockRepository;
import com.gst.billingandstockmanagement.repository.UserRepository;
import com.gst.billingandstockmanagement.utils.PaginationUtils;
import com.gst.billingandstockmanagement.specifications.SpecificationBuilder;

import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class StockServiceImpl implements StockService {
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
	ProductRepository productRepository;
    
    @Autowired
    UserRepository userRepository;

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
                stockRepository.save(existingStock);
            } else {
                // If the batch number or expiry date is different, create a new stock entry
                Stock stock = new Stock();
                stock.setUser(user);
                stock.setProduct(product);
                stock.setQuantity(stockDTO.getQuantity());
                stock.setBatchNo(stockDTO.getBatchNo());
                stock.setExpiryDate(stockDTO.getExpiryDate());
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



}