package com.billingapp.service;

public interface EmailService {
    void sendEmailWithAttachment(String to, String subject, String body, byte[] pdfBytes, String fileName) throws Exception;
}