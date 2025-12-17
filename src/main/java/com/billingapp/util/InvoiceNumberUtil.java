package com.billingapp.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InvoiceNumberUtil {

    /**
     * Generates a readable invoice number.
     * Format: YYYYMM-<5-digit-random> e.g. 202512-04213
     * Simple and collision-resistant for development.
     */
    public static String generate() {
        String prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int seq = (int)(System.currentTimeMillis() % 100000); // quick sequence
        return String.format("%s-%05d", prefix, Math.abs(seq));
    }
}
