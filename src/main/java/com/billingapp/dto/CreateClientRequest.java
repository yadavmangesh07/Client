package com.billingapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientRequest {
    @NotBlank(message = "Client name is required")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String address;

    // 👇 RENAME THIS to 'gstin'
    private String gstin; 

    private String notes;
    private String state;
    private String stateCode;
    private String pincode;
}