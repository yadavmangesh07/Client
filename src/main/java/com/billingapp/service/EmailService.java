package com.billingapp.service;

public interface EmailService {
    void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String filename) throws Exception;
}