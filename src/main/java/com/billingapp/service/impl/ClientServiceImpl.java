package com.billingapp.service.impl;

import com.billingapp.dto.ClientDTO;
import com.billingapp.dto.CreateClientRequest;
import com.billingapp.entity.Client;
import com.billingapp.mapper.ClientMapper;
import com.billingapp.repository.ClientRepository;
import com.billingapp.service.ClientService;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper mapper;
    private final MongoTemplate mongoTemplate;

    public ClientServiceImpl(ClientRepository clientRepository, ClientMapper mapper, MongoTemplate mongoTemplate) {
        this.clientRepository = clientRepository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ClientDTO create(CreateClientRequest req) {
        Client client = Client.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .address(req.getAddress())
                .gstin(req.getGstin()) // 👈 Mapping gstNo (DTO) -> gstin (Entity)
                .notes(req.getNotes())
                .state(req.getState())
                .stateCode(req.getStateCode())
                .pincode(req.getPincode())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Client saved = clientRepository.save(client);
        return mapper.toDto(saved);
    }

    @Override
    public ClientDTO update(String id, CreateClientRequest req) {
        System.out.println("DEBUG UPDATE: Received Request for ID: " + id);
    System.out.println("DEBUG UPDATE: Payload State: " + req.getState());
    System.out.println("DEBUG UPDATE: Payload StateCode: " + req.getStateCode());
    System.out.println("DEBUG UPDATE: Payload GSTIN: " + req.getGstin());
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        
        client.setName(req.getName());
        client.setEmail(req.getEmail());
        client.setPhone(req.getPhone());
        client.setAddress(req.getAddress());
        client.setGstin(req.getGstin()); // 👈 Update GSTIN
        client.setNotes(req.getNotes());
        client.setState(req.getState());
        client.setStateCode(req.getStateCode());
        client.setPincode(req.getPincode());
        client.setUpdatedAt(Instant.now());
        
        Client saved = clientRepository.save(client);
        return mapper.toDto(saved);
    }

    @Override
    public ClientDTO getById(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        return mapper.toDto(client);
    }

    @Override
    public List<ClientDTO> getAll() {
        return clientRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        clientRepository.deleteById(id);
    }

    // --------------------------
    // Optimized Search Implementation
    // --------------------------
    @Override
    public Page<ClientDTO> search(String q, int page, int size, String sort) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Sort s = parseSort(sort, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Query query = new Query(); // 1. Build Base Query

        if (q != null && !q.isBlank()) {
            String regex = ".*" + q.trim() + ".*";
            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("email").regex(regex, "i")
            );
            query.addCriteria(criteria);
        }

        // 2. Count Total (Before adding pagination)
        long total = mongoTemplate.count(query, Client.class);

        // 3. Apply Pagination & Sort
        query.with(pageable);

        // 4. Fetch Data
        List<Client> list = mongoTemplate.find(query, Client.class);
        List<ClientDTO> dtos = list.stream().map(mapper::toDto).collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, total);
    }

    private Sort parseSort(String sort, Sort defaultSort) {
        if (sort == null || sort.isBlank()) return defaultSort;
        try {
            String[] parts = sort.split(",");
            String prop = parts[0];
            Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            return Sort.by(dir, prop);
        } catch (Exception e) {
            return defaultSort;
        }
    }
}