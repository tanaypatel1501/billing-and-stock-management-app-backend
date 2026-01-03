package com.gst.billingandstockmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BillingAndStockManagement {

	public static void main(String[] args) {
		SpringApplication.run(BillingAndStockManagement.class, args);
	}

}
