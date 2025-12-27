package com.billingapp.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "projects")
public class Project {

    @Id
    private String id;
    private String clientId; // Link to the Client
    private String title;    // e.g., "Living Room Renovation"
    private String description;
    private String status;   // ONGOING, COMPLETED, ARCHIVED

    // We store the files here
    private List<ProjectDocument> documents = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    // Nested class to define a single file
    @Data
    public static class ProjectDocument {
        private String fileId; // Random ID for UI keys
        private String name;   // "Site_Measurements.pdf"
        private String url;    // The Firebase Link
        private String type;   // "image/png", "application/pdf"
        private Instant uploadedAt = Instant.now();
    }
}