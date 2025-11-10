package com.gst.billingandstockmanagement.services.product;

import com.gst.billingandstockmanagement.dto.ProductDTO;
import java.util.List;

public interface ProductService {

	ProductDTO addProduct(ProductDTO productDTO);

    List<ProductDTO> getAllProducts();
    
    ProductDTO getProductById(Long productId);
    
    void deleteProductById(Long productId);
    
    ProductDTO editProduct(Long productId, ProductDTO productDTO);
}
