package com.billingapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String filename;   // stored filename (unique)
    private String originalName; // original uploaded name
    private String url;        // accessible URL (e.g. /files/{filename})
    private String contentType;
    private long size;
    private Instant uploadedAt;
}
