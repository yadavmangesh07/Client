package com.billingapp.service.impl;

import com.billingapp.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) throws Exception {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int i = original.lastIndexOf('.');
        if (i >= 0) ext = original.substring(i);
        String filename = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
        Path target = this.rootLocation.resolve(filename);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new IOException("Failed to store file " + original, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) throws Exception {
        Path file = load(filename);
        Resource resource = new PathResource(file);
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Could not read file: " + filename);
        }
        return resource;
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename).normalize();
    }
}
