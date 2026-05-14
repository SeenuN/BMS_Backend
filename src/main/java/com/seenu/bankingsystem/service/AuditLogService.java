package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.AuditLog;
import com.seenu.bankingsystem.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Call this from any service/controller to record an event.
     */
    public void log(String username, String action, String entityType,
                    Long entityId, String description, String ipAddress, String status) {
        AuditLog entry = new AuditLog(
                username, action, entityType, entityId, description, ipAddress, status
        );
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> getLogs(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return auditLogRepository
                    .findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(search, pageable);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
