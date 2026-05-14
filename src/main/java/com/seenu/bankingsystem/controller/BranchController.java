package com.seenu.bankingsystem.controller;

import com.seenu.bankingsystem.entity.Branch;
import com.seenu.bankingsystem.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    @Autowired
    private BranchService branchService;

    @GetMapping
    public List<Branch> getAllBranches() {
        return branchService.getAllBranches();
    }
}
