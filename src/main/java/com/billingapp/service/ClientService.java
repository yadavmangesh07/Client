package com.billingapp.service;

import com.billingapp.dto.ClientDTO;
import com.billingapp.dto.CreateClientRequest;
//import org.springframework.data.domain.Page;

import java.util.List;

public interface ClientService {
    ClientDTO create(CreateClientRequest req);
    ClientDTO getById(String id);
    List<ClientDTO> getAll();
    ClientDTO update(String id, CreateClientRequest req);
    void delete(String id);

    // new:
    org.springframework.data.domain.Page<ClientDTO> search(String q, int page, int size, String sort);
}
