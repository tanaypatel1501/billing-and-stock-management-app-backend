package com.gst.billingandstockmanagement.controllers;

import com.gst.billingandstockmanagement.configuration.KoyebWarmupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class WarmupStatusController {

    @Autowired
    private KoyebWarmupService koyebWarmupService;

    @GetMapping("/render-status")
    public ResponseEntity<Map<String, String>> renderStatus() {
        return ResponseEntity.ok(Map.of(
                "state", koyebWarmupService.getState().name()
        ));
    }
}