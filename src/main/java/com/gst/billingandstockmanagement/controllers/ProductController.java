package com.gst.billingandstockmanagement.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

import com.gst.billingandstockmanagement.dto.ProductDTO;
import com.gst.billingandstockmanagement.dto.BulkProductResponse;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.services.product.ProductService;


@RestController
@RequestMapping("/api/product")
public class ProductController {
	
	@Autowired
	private ProductService productService;
	
	@PostMapping("/add")
    public ProductDTO addProduct(@RequestBody ProductDTO productDTO) {
        return productService.addProduct(productDTO);
    }
	
	@GetMapping("/all")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        SearchRequest req = new SearchRequest();
        req.setPage(page);
        req.setSize(size);
        Page<Product> p = productService.searchWithPagination(req);
        return ResponseEntity.ok(p);
    }
	
	@GetMapping("/{productId}")
    public ProductDTO getProductById(@PathVariable Long productId) {
        // Use the ProductService to fetch a product by its ID
        return productService.getProductById(productId);
    }
	
	@DeleteMapping("/delete/{productId}")
    public void deleteProductById(@PathVariable Long productId) {
        // Use the ProductService to delete a product by its ID
        productService.deleteProductById(productId);
    }
	
	@PutMapping("/edit/{productId}")
    public ProductDTO editProduct(@PathVariable Long productId, @RequestBody ProductDTO productDTO) {
        // Use the ProductService to edit the product with the given ID
        return productService.editProduct(productId, productDTO);
    }

    @PostMapping(path = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BulkProductResponse uploadBulkProducts(@RequestParam("file") MultipartFile file) {
        return productService.handleBulkUpload(file);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(@RequestBody SearchRequest request) {
        Page<Product> p = productService.searchWithPagination(request);
        return ResponseEntity.ok(p);
    }
}
