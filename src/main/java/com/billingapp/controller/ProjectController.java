package com.billingapp.controller;

import com.billingapp.entity.Project;
import com.billingapp.repository.ProjectRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin("*")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // 1. Get all projects for a specific Client
    @GetMapping("/client/{clientId}")
    public List<Project> getClientProjects(@PathVariable String clientId) {
        return projectRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    // 2. Create a new Project
    @PostMapping
    public Project createProject(@RequestBody Project project) {
        project.setCreatedAt(Instant.now());
        if(project.getStatus() == null) project.setStatus("ONGOING");
        return projectRepository.save(project);
    }

    // 3. Attach a File Link (Frontend sends the Firebase URL here)
    @PostMapping("/{projectId}/documents")
    public Project addDocument(@PathVariable String projectId, @RequestBody Project.ProjectDocument doc) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        doc.setFileId(UUID.randomUUID().toString());
        doc.setUploadedAt(Instant.now());
        
        project.getDocuments().add(doc);
        return projectRepository.save(project);
    }

    // 4. Delete a File Link
    @DeleteMapping("/{projectId}/documents/{fileId}")
    public Project removeDocument(@PathVariable String projectId, @PathVariable String fileId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.getDocuments().removeIf(doc -> doc.getFileId().equals(fileId));
        return projectRepository.save(project);
    }
    
    // 5. Delete Project
    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable String id) {
        projectRepository.deleteById(id);
    }
}