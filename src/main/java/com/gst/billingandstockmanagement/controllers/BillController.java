package com.gst.billingandstockmanagement.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import com.gst.billingandstockmanagement.dto.BillDTO;
import com.gst.billingandstockmanagement.dto.SearchRequest;
import com.gst.billingandstockmanagement.services.bill.BillService;
import com.gst.billingandstockmanagement.entities.Bill;


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
    public ResponseEntity<Page<Bill>> getAllBills(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        SearchRequest req = new SearchRequest();
        req.setPage(page);
        req.setSize(size);
        Page<Bill> p = billService.searchWithPagination(req);
        return ResponseEntity.ok(p);
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

    @PostMapping("/search")
    public ResponseEntity<Page<Bill>> searchBills(@RequestBody SearchRequest request) {
        Page<Bill> p = billService.searchWithPagination(request);
        return ResponseEntity.ok(p);
    }
}
