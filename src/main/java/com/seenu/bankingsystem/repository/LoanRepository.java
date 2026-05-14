package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);
    Page<Loan> findAllByOrderByAppliedAtDesc(Pageable pageable);
}