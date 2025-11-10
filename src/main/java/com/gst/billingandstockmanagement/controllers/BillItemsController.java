package com.gst.billingandstockmanagement.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gst.billingandstockmanagement.dto.BillItemsDTO;
import com.gst.billingandstockmanagement.services.billItems.BillItemsService;

@RestController
@RequestMapping("/api/bill_items")
public class BillItemsController {
    @Autowired
    private BillItemsService billItemsService;

    @PostMapping("/add")
    public void addBillItems(@RequestBody BillItemsDTO billItemsDTO) {
        billItemsService.addBillItems(billItemsDTO);
    }
}
