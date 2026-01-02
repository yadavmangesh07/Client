package com.billingapp.dto;

import com.billingapp.entity.Client;
import com.billingapp.entity.Invoice;
import com.billingapp.entity.WorkCompletionCertificate;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClientProfileDTO {
    private Client client;
    private List<Invoice> recentInvoices;
    private List<WorkCompletionCertificate> recentCertificates;
    
    // Stats specific to this client
    private Map<String, Object> stats; 
}