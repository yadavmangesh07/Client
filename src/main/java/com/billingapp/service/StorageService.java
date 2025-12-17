package com.billingapp.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {

    /**
     * Save the uploaded file and return the stored filename (unique).
     */
    String store(MultipartFile file) throws Exception;

    /**
     * Load a file as a Spring Resource by stored filename.
     */
    Resource loadAsResource(String filename) throws Exception;

    /**
     * Return the absolute Path for a filename.
     */
    Path load(String filename);
}
