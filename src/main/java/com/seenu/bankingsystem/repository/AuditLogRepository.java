package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByUsernameContainingIgnoreCaseOrderByCreatedAtDesc(
            String username, Pageable pageable);
}