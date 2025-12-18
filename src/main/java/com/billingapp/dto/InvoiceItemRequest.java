package com.billingapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvoiceItemRequest {
    @NotBlank(message = "Item description is required")
    private String description;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int qty;

    @Min(value = 0, message = "Rate must be non-negative")
    private double rate;
    private String hsnCode;
    private String uom;
    private double taxRate;
}
