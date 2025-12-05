package com.gst.billingandstockmanagement.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.services.bill.BillService;


@RestController
@RequestMapping("/api/bill")
public class BillController {
	
	@Autowired
    private BillService billService;

	@PostMapping("/add")
	public ResponseEntity<BillDTO> addBill(@RequestBody BillDTO billDTO) {
	    BillDTO createdBill = billService.addBill(billDTO);
	    return new ResponseEntity<>(createdBill, HttpStatus.CREATED);
	}

    
    @GetMapping("/{billId}")
    public ResponseEntity<BillDTO> getBillById(@PathVariable Long billId) {
        BillDTO dto = billService.getBillById(billId);
        if (dto == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
    
    @GetMapping("/all")
    public List<BillDTO> getAllBills() {
        return billService.getAllBills();
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BillDTO>> getAllBillsByUser(@PathVariable Long userId) {
        List<BillDTO> userBills = billService.getAllBillsByUser(userId);
        return new ResponseEntity<>(userBills, HttpStatus.OK);
    }
    
    @DeleteMapping("/delete/{billId}")
    public void deleteBill(@PathVariable Long billId) {
        billService.deleteBill(billId);
    }
}
