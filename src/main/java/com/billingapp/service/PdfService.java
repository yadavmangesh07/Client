package com.billingapp.service;

import java.io.ByteArrayOutputStream;

public interface PdfService {

    /**
     * Generate a PDF for the given invoice id and return PDF bytes.
     */
    ByteArrayOutputStream generateInvoicePdf(String invoiceId) throws Exception;
}
