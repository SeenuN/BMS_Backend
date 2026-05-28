package com.seenu.bankingsystem.repository;

import com.seenu.bankingsystem.entity.SmsPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsPreferenceRepository extends JpaRepository<SmsPreference, Long> {

    Optional<SmsPreference> findByUserId(Long userId);
}
