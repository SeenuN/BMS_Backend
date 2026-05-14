package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.FraudAlert;
import com.seenu.bankingsystem.repository.FraudAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud-alerts")
public class FraudAlertController {

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    /** Admin: all PENDING alerts, newest first */
    @GetMapping("/pending")
    public List<FraudAlert> getPending() {
        return fraudAlertRepository.findByStatus("PENDING");
    }

    /** Admin: all alerts (any status), newest first */
    @GetMapping
    public List<FraudAlert> getAll() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Admin: alerts scoped to one account */
    @GetMapping("/account/{accountId}")
    public List<FraudAlert> getByAccount(@PathVariable Long accountId) {
        return fraudAlertRepository.findByAccountId(accountId);
    }

    /** Admin: update an alert's status to REVIEWED or DISMISSED */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return fraudAlertRepository.findById(id).map(alert -> {
            alert.setStatus(newStatus);
            return ResponseEntity.ok(fraudAlertRepository.save(alert));
        }).orElse(ResponseEntity.notFound().build());
    }
}
