package com.billingapp.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    /**
     * 🟢 FIX: Return instantly from memory. 
     * Bypassing MongoDB ensures this endpoint never blocks or times out,
     * allowing Render to register the app as "healthy" immediately during cold starts.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> lightHealthCheck() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("status", "UP");
        status.put("server", "warm");
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
}