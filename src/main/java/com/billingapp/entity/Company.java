package com.billingapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "company_profile")
public class Company {

    @Id
    private String id; // We will usually keep this as "1" or a constant ID since there's only 1 company.

    // Basic Info
    private String companyName;
    private String address;
    private String phone;
    private String email;
    private String website;

    // Tax Info
    private String gstin;
    private String udyamRegNo; // From your screenshot

    // Bank Details
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String ifscCode;
    private String branch;
    private String pincode;
    private String secondaryEmail;
    private String secondaryPhone;

    // Branding (URLs from StorageService)
    private String logoUrl;
    private String signatureUrl;

}