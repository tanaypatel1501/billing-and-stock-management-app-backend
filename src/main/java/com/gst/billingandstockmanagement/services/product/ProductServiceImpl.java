package com.gst.billingandstockmanagement.services.product;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gst.billingandstockmanagement.dto.ProductDTO;
import com.gst.billingandstockmanagement.dto.BulkProductResponse;
import com.gst.billingandstockmanagement.dto.BulkRowResult;
import com.gst.billingandstockmanagement.entities.Product;
import com.gst.billingandstockmanagement.repository.ProductRepository;
import com.gst.billingandstockmanagement.utils.FileParser;

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

    @Override
    public BulkProductResponse handleBulkUpload(MultipartFile file) {
        BulkProductResponse resp = new BulkProductResponse();
        List<BulkRowResult> errors = new ArrayList<>();
        int created = 0, updated = 0, processed = 0;
        try {
            List<Map<String, String>> rows = FileParser.parse(file);
            processed = rows.size();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, String> row = rows.get(i);
                int rowNo = i + 2; // header at row 1
                try {
                    // Validate identifier: id or (name + hsn + packing)
                    String idStr = row.getOrDefault("id", "").trim();
                    String name = blankToNull(row.getOrDefault("name", ""));
                    String hsn = blankToNull(row.getOrDefault("hsn", ""));
                    // --- ADDED: Extract Packing ---
                    String packing = blankToNull(row.getOrDefault("packing", ""));

                    // --- UPDATED: Check for all 3 fields for unique identifier ---
                    if (idStr.isEmpty() && (name == null || hsn == null || packing == null)) {
                        throw new RuntimeException("Missing identifier: provide 'id' or 'name', 'hsn', and 'packing'");
                    }

                    // Validate numeric fields (rest of validation remains the same)
                    Double mrp = null, cgst = null, sgst = null;
                    if (row.containsKey("mrp") && !row.get("mrp").trim().isEmpty()) mrp = parseDouble(row.get("mrp"));
                    if (row.containsKey("cgst") && !row.get("cgst").trim().isEmpty()) cgst = parseDouble(row.get("cgst"));
                    if (row.containsKey("sgst") && !row.get("sgst").trim().isEmpty()) sgst = parseDouble(row.get("sgst"));
                    if (cgst != null && (cgst < 0 || cgst > 100)) throw new RuntimeException("CGST out of range (0-100): " + cgst);
                    if (sgst != null && (sgst < 0 || sgst > 100)) throw new RuntimeException("SGST out of range (0-100): " + sgst);

                    Product target = null;
                    if (!idStr.isEmpty()) {
                        // Logic for finding by ID remains the same
                        Long id = null;
                        try { id = Long.valueOf(idStr); } catch (Exception e) { throw new RuntimeException("Invalid id value"); }
                        Optional<Product> opt = productRepository.findById(id);
                        if (opt.isPresent()) {
                            target = opt.get();
                        } else {
                            throw new RuntimeException("Product with id=" + id + " not found");
                        }
                    } else {
                        // --- UPDATED: Try to find by name+hsn+packing (case-insensitive) ---
                        Optional<Product> opt = productRepository.findByNameIgnoreCaseAndHSNAndPacking(name, hsn, packing);
                        if (opt.isPresent()) {
                            target = opt.get();
                        } else {
                            // insert new: require name, hsn, and packing
                            if (name == null || hsn == null || packing == null) throw new RuntimeException("Missing name, hsn, or packing for insert");
                            target = new Product();
                        }
                    }

                    // Apply fields (respect empty cells: don't overwrite existing unless provided)
                    applyRowToProductWithValidation(row, target);

                    if (target.getId() == null) {
                        productRepository.save(target);
                        created++;
                    } else {
                        productRepository.save(target);
                        updated++;
                    }
                } catch (Exception ex) {
                    BulkRowResult br = new BulkRowResult();
                    br.setRowNumber(rowNo);
                    br.setRow(row);
                    br.setErrorMessage(ex.getMessage());
                    errors.add(br);
                }
            }
        } catch (Exception e) {
            BulkRowResult br = new BulkRowResult();
            br.setRowNumber(0);
            br.setRow(null);
            br.setErrorMessage("Failed to parse file: " + e.getMessage());
            errors.add(br);
        }
        resp.setProcessed(processed);
        resp.setCreated(created);
        resp.setUpdated(updated);
        resp.setFailed(errors.size());
        resp.setErrors(errors);
        return resp;
    }

	private void applyRowToProductWithValidation(Map<String, String> row, Product p) {
		// Only set fields that are present (non-empty) in the incoming row
		if (row.containsKey("name") && row.get("name").trim().length() > 0) p.setName(blankToNull(row.get("name")));
		if (row.containsKey("hsn") && row.get("hsn").trim().length() > 0) p.setHSN(blankToNull(row.get("hsn")));
		if (row.containsKey("mrp") && row.get("mrp").trim().length() > 0) p.setMRP(parseDouble(row.get("mrp")));
		if (row.containsKey("cgst") && row.get("cgst").trim().length() > 0) p.setCGST(parseDouble(row.get("cgst")));
		if (row.containsKey("sgst") && row.get("sgst").trim().length() > 0) p.setSGST(parseDouble(row.get("sgst")));
		if (row.containsKey("packing") && row.get("packing").trim().length() > 0) p.setPacking(blankToNull(row.get("packing")));
	}

	private String blankToNull(String s) { return s == null ? null : (s.trim().isEmpty() ? null : s.trim()); }
	private Double parseDouble(String s) { try { if (s == null || s.trim().isEmpty()) return null; return Double.valueOf(s.trim()); } catch (Exception e) { throw new RuntimeException("Invalid numeric value: '" + s + "'"); } }

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
