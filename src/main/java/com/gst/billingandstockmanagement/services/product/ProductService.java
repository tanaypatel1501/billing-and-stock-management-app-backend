package com.gst.billingandstockmanagement.services.product;

import com.gst.billingandstockmanagement.dto.ProductDTO;
import com.gst.billingandstockmanagement.dto.BulkProductResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ProductService {

	ProductDTO addProduct(ProductDTO productDTO);

    List<ProductDTO> getAllProducts();
    
    ProductDTO getProductById(Long productId);
    
    void deleteProductById(Long productId);
    
    ProductDTO editProduct(Long productId, ProductDTO productDTO);

    // Bulk upload products from CSV or Excel file. Returns a report with per-row errors.
    BulkProductResponse handleBulkUpload(MultipartFile file);
}
