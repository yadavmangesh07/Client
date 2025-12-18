package com.billingapp.controller;

import com.billingapp.entity.Company;
import com.billingapp.repository.CompanyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private static final String PROFILE_ID = "MY_COMPANY";

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public ResponseEntity<Company> getProfile() {
        // Return existing or empty object if not set yet
        return ResponseEntity.ok(
            companyRepository.findById(PROFILE_ID).orElse(new Company())
        );
    }

    @PostMapping
    public ResponseEntity<Company> saveProfile(@RequestBody Company company) {
        // Force the ID to be constant so we don't create multiple companies
        company.setId(PROFILE_ID);
        return ResponseEntity.ok(companyRepository.save(company));
    }
}