package com.billingapp.service;

public interface PdfService {
    // ❌ OLD: ByteArrayOutputStream generateInvoicePdf(...)
    // ✅ NEW: Return byte[]
    byte[] generateInvoicePdf(String invoiceId) throws Exception;
}