package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.AuditLog;
import com.seenu.bankingsystem.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public Page<AuditLog> getLogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditLogService.getLogs(search, page, size);
    }
}
