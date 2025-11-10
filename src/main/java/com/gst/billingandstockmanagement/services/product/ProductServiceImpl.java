package com.gst.billingandstockmanagement.services.product;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gst.billingandstockmanagement.dto.ProductDTO;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	ProductRepository productRepository;

	@Override
	public ProductDTO addProduct(ProductDTO productDTO) {
		Product product = new Product();
        product.setName(productDTO.getName());
        product.setHSN(productDTO.getHSN());
        product.setMRP(productDTO.getMRP());
        product.setCGST(productDTO.getCGST());
        product.setSGST(productDTO.getSGST());
        product.setPacking(productDTO.getPacking());

        Product savedProduct = productRepository.save(product);

        ProductDTO savedProductDTO = new ProductDTO();
        savedProductDTO.setId(savedProduct.getId());
        savedProductDTO.setName(savedProduct.getName());
        savedProductDTO.setHSN(savedProduct.getHSN());
        savedProductDTO.setMRP(savedProduct.getMRP());
        savedProductDTO.setCGST(savedProduct.getCGST());
        savedProductDTO.setSGST(savedProduct.getSGST());
        savedProductDTO.setPacking(savedProduct.getPacking());

        return savedProductDTO;
    }
	
	@Override
	public List<ProductDTO> getAllProducts() {
		List<Product> products = productRepository.findAll();

		// Map the list of Product entities to ProductDTOs
		List<ProductDTO> productDTOs = products.stream()
				.map(this::mapProductToProductDTO)
				.collect(Collectors.toList());

		return productDTOs;
	}
	
	@Override
	public ProductDTO getProductById(Long productId) {
		Optional<Product> optionalProduct = productRepository.findById(productId);
		if (optionalProduct.isPresent()) {
			Product product = optionalProduct.get();
			return mapProductToProductDTO(product);
		} else {
			System.out.println("Product Not Found");
			return null;
		}
	}
	
	@Override
	public void deleteProductById(Long productId) {
		// Delete the product by ID
		productRepository.deleteById(productId);
	}

	@Override
	public ProductDTO editProduct(Long productId, ProductDTO productDTO) {
		Optional<Product> optionalProduct = productRepository.findById(productId);
		if (optionalProduct.isPresent()) {
			Product existingProduct = optionalProduct.get();
			
			// Update the fields of the existing product with the values from the DTO
			existingProduct.setName(productDTO.getName());
			existingProduct.setHSN(productDTO.getHSN());
			existingProduct.setMRP(productDTO.getMRP());
			existingProduct.setCGST(productDTO.getCGST());
			existingProduct.setSGST(productDTO.getSGST());
			existingProduct.setPacking(productDTO.getPacking());

			// Save the updated product
			Product updatedProduct = productRepository.save(existingProduct);

			// Map the updated product to a DTO and return it
			return mapProductToProductDTO(updatedProduct);
		} else {
			System.out.println("Product Not Found");
			return null;
		}
	}
	
	private ProductDTO mapProductToProductDTO(Product product) {
		ProductDTO productDTO = new ProductDTO();
		productDTO.setId(product.getId());
		productDTO.setName(product.getName());
		productDTO.setHSN(product.getHSN());
		productDTO.setMRP(product.getMRP());
		productDTO.setCGST(product.getCGST());
		productDTO.setSGST(product.getSGST());
		productDTO.setPacking(product.getPacking());

		return productDTO;
	}
}
