package com.gst.billingandstockmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.gst.billingandstockmanagement.entities.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{

}
