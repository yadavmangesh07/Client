package com.billingapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "clients")
public class Client {

    @Id
    private String id;

    private String name;
    private String email;
    private String phone;
    private String address;
    private String notes;

    private Instant createdAt;
    private Instant updatedAt;
    
    private String gstin;      // Client's Tax Number
    private String state;      // e.g. Gujarat
    private String stateCode;  // e.g. 24
}
