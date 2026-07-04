package com.billingapp.service.impl;

import com.billingapp.service.StorageService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("Initializing system local storage directory sub-engine context at path: {}", this.rootLocation);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.error("Critical storage sub-system boundary failure: Unable to allocate directory layout path: " + this.rootLocation, e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            log.warn("File write process aborted: target multipart attachment file payload is empty or null");
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename());
        log.info("Attempting to write inbound multipart asset file stream into local storage context: {}", original);

        String ext = "";
        int i = original.lastIndexOf('.');
        if (i >= 0) ext = original.substring(i);
        
        String filename = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
        Path target = this.rootLocation.resolve(filename);
        
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Asset stream file successfully persisted to absolute host location path as target mapping name: {}", filename);
            return filename;
        } catch (IOException e) {
            log.error("I/O subsystem transmission error writing asset binary payload stream " + original + " to disk location: " + target, e);
            throw new IOException("Failed to store file " + original, e);
        }
    }

    @Override
    public Resource loadAsResource(String filename) throws Exception {
        log.info("Request received to extract resource storage token entity block matching target name: {}", filename);
        
        Path file = load(filename);
        Resource resource = new PathResource(file);
        
        if (!resource.exists() || !resource.isReadable()) {
            log.error("Disk block extraction operation failed: asset file name {} non-existent or restricted inside root location directory bounds", filename);
            throw new IOException("Could not read file: " + filename);
        }
        
        log.debug("Resource token matching target asset reference name successfully resolved to resource pointer");
        return resource;
    }

    @Override
    public Path load(String filename) {
        log.debug("Resolving coordinate tracking vector paths inside root allocation pool layout space for target entity: {}", filename);
        return rootLocation.resolve(filename).normalize();
    }
}