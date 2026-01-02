package com.billingapp.service;

import com.billingapp.dto.ClientDTO;
import com.billingapp.dto.ClientProfileDTO; // 👈 Make sure to import this
import com.billingapp.dto.CreateClientRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateClientRequest req);
    ClientDTO getById(String id);
    List<ClientDTO> getAll();
    ClientDTO update(String id, CreateClientRequest req);
    void delete(String id);

    Page<ClientDTO> search(String q, int page, int size, String sort);

    // 👇 NEW: Optimized Profile Endpoint
    ClientProfileDTO getClientProfile(String id);
}