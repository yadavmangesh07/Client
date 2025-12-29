package com.billingapp.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class ClientDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private String gstin;
    private String state;
    private String stateCode;
    private String pincode;
}
