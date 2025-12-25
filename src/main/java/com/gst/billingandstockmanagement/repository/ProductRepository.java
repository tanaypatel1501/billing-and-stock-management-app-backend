package com.gst.billingandstockmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.gst.billingandstockmanagement.entities.Product;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>{
    Optional<Product> findByNameIgnoreCaseAndHSNAndPacking(String name, String hsn, String packing);
}
