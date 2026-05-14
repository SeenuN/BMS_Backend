package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByAccountId(Long accountId);
    List<FraudAlert> findByStatus(String status);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}
