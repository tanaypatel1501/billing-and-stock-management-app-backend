package com.gst.billingandstockmanagement.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gst.billingandstockmanagement.dto.ProductDTO;
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
    public List<ProductDTO> getAllProducts() {
        // Use the ProductService to fetch all products from the database
        return productService.getAllProducts();
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
}
