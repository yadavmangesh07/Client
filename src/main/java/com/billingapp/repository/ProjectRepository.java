package com.billingapp.repository;

import com.billingapp.entity.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findByClientIdOrderByCreatedAtDesc(String clientId);
}