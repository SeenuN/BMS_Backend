package com.seenu.bankingsystem.service;

import com.seenu.bankingsystem.entity.Beneficiary;
import com.seenu.bankingsystem.repository.BeneficiaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BeneficiaryService {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    public Beneficiary addBeneficiary(Beneficiary beneficiary) {
        return beneficiaryRepository.save(beneficiary);
    }

    public List<Beneficiary> getBeneficiariesByUserId(Long userId) {
        return beneficiaryRepository.findByUserId(userId);
    }

    public Beneficiary updateBeneficiary(Long id, Beneficiary beneficiaryDetails) {
        Optional<Beneficiary> optionalBeneficiary = beneficiaryRepository.findById(id);
        if (optionalBeneficiary.isPresent()) {
            Beneficiary existing = optionalBeneficiary.get();
            existing.setName(beneficiaryDetails.getName());
            existing.setAccountNumber(beneficiaryDetails.getAccountNumber());
            existing.setBankName(beneficiaryDetails.getBankName());
            existing.setIfscCode(beneficiaryDetails.getIfscCode());
            existing.setNickname(beneficiaryDetails.getNickname());
            return beneficiaryRepository.save(existing);
        }
        throw new RuntimeException("Beneficiary not found with id " + id);
    }

    public void deleteBeneficiary(Long id) {
        beneficiaryRepository.deleteById(id);
    }
}
