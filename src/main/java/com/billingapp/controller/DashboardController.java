package com.billingapp.controller;

import com.billingapp.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard") // Matches frontend client mapping bases
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        
        if (force) {
            log.info("Manual dashboard analytics refresh requested by user: forcing state eviction sequence");
            dashboardService.clearDashboardCache();
        }
        
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
}