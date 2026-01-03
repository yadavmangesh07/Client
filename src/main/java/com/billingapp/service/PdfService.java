package com.billingapp.service;

public interface PdfService {
   
    byte[] generateInvoicePdf(String invoiceId) throws Exception;
} 