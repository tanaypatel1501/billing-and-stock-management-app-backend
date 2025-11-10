package com.gst.billingandstockmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gst.billingandstockmanagement.entities.BillItems;

public interface BillItemsRepository extends JpaRepository<BillItems, Long>{

}
