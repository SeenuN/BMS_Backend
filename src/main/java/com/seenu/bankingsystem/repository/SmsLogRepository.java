package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    List<SmsLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SmsLog> findByStatus(String status);

    List<SmsLog> findAllByOrderByCreatedAtDesc();
}
