package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.Beneficiary;
import com.seenu.bankingsystem.service.BeneficiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    @Autowired
    private BeneficiaryService beneficiaryService;

    @PostMapping("/add")
    public Beneficiary addBeneficiary(@RequestBody Beneficiary beneficiary) {
        return beneficiaryService.addBeneficiary(beneficiary);
    }

    @GetMapping("/user/{userId}")
    public List<Beneficiary> getBeneficiariesByUser(@PathVariable Long userId) {
        return beneficiaryService.getBeneficiariesByUserId(userId);
    }

    @PutMapping("/update/{id}")
    public Beneficiary updateBeneficiary(@PathVariable Long id, @RequestBody Beneficiary beneficiary) {
        return beneficiaryService.updateBeneficiary(id, beneficiary);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteBeneficiary(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
        return "Beneficiary deleted successfully";
    }
}
