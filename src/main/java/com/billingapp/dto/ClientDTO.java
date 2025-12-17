package com.billingapp.dto;

import lombok.Data;

@Data
public class ClientDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String notes;
}
