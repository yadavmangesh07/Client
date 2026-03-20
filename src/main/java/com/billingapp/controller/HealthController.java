package com.billingapp.controller;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> deepHealthCheck() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 🟢 The "Magic" Command: Pings MongoDB without fetching heavy data
            Document ping = new Document("ping", 1);
            mongoTemplate.getDb().runCommand(ping);
            
            status.put("database", "connected");
            status.put("server", "warm");
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("database", "disconnected");
            status.put("error", e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
}