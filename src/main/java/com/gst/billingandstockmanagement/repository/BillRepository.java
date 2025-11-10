package com.gst.billingandstockmanagement.repository;

import com.gst.billingandstockmanagement.entities.Bill;
import com.gst.billingandstockmanagement.entities.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BillRepository extends JpaRepository<Bill, Long> {

    // Custom query to calculate the total amount of a bill based on its bill items
    @Query("SELECT SUM(bi.amount) FROM BillItems bi WHERE bi.bill = ?1")
    Double calculateTotalAmount(Bill bill);

	List<Bill> findByUser(User user);

    // Other custom queries or methods can go here
}
