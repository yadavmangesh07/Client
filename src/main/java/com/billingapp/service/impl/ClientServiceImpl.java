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
                .notes(req.getNotes())
                .createdAt(Instant.now())
                .build();

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
    public ClientDTO update(String id, CreateClientRequest req) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        client.setName(req.getName());
        client.setEmail(req.getEmail());
        client.setPhone(req.getPhone());
        client.setAddress(req.getAddress());
        client.setNotes(req.getNotes());
        client.setUpdatedAt(Instant.now());
        Client saved = clientRepository.save(client);
        return mapper.toDto(saved);
    }

    @Override
    public void delete(String id) {
        clientRepository.deleteById(id);
    }

    // --------------------------
    // Search (fuzzy by name/email) with pagination & sort
    // --------------------------
    @Override
    public Page<ClientDTO> search(String q, int page, int size, String sort) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Sort s = parseSort(sort, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Query query = new Query().with(pageable);
        if (q != null && !q.isBlank()) {
            String regex = ".*" + q.trim() + ".*";
            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("email").regex(regex, "i")
            );
            query.addCriteria(criteria);
        }
        long total = mongoTemplate.count(query.skip(-1).limit(-1), Client.class); // count without pageable
        // apply pageable properly:
        query = new Query();
        if (q != null && !q.isBlank()) {
            String regex = ".*" + q.trim() + ".*";
            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("email").regex(regex, "i")
            );
            query.addCriteria(criteria);
        }
        query.with(pageable);
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
