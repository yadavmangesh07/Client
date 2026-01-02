package com.billingapp.service.impl;

import com.billingapp.dto.ClientDTO;
import com.billingapp.dto.ClientProfileDTO;
import com.billingapp.dto.CreateClientRequest;
import com.billingapp.entity.Client;
import com.billingapp.entity.Invoice;
import com.billingapp.entity.WorkCompletionCertificate;
import com.billingapp.mapper.ClientMapper;
import com.billingapp.repository.ClientRepository;
import com.billingapp.repository.InvoiceRepository;
import com.billingapp.repository.WorkCompletionCertificateRepository;
import com.billingapp.service.ClientService;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;       // 👈 Added
    private final WorkCompletionCertificateRepository wccRepository; // 👈 Added
    private final ClientMapper mapper;
    private final MongoTemplate mongoTemplate;

    // Updated Constructor with new repositories
    public ClientServiceImpl(ClientRepository clientRepository,
                             InvoiceRepository invoiceRepository,
                             WorkCompletionCertificateRepository wccRepository,
                             ClientMapper mapper,
                             MongoTemplate mongoTemplate) {
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
        this.wccRepository = wccRepository;
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
                .gstin(req.getGstin())
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
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        
        client.setName(req.getName());
        client.setEmail(req.getEmail());
        client.setPhone(req.getPhone());
        client.setAddress(req.getAddress());
        client.setGstin(req.getGstin());
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

    // 👇 NEW: Optimized Profile Endpoint Implementation
    @Override
    public ClientProfileDTO getClientProfile(String clientId) {
        // 1. Fetch Client Basic Details (Fast & Required first for the name lookup)
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // 2. Define Parallel Tasks
        
        // Task A: Fetch Invoices by Client ID
        CompletableFuture<List<Invoice>> invoicesTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.findByClientId(clientId)
        );

        // Task B: Fetch WCCs by Client Name (Store Name)
        // Using the name from the fetched client to ensure accuracy
        CompletableFuture<List<WorkCompletionCertificate>> wccTask = CompletableFuture.supplyAsync(() -> 
            wccRepository.findByStoreNameIgnoreCase(client.getName())
        );

        // 3. Wait for all tasks to complete
        CompletableFuture.allOf(invoicesTask, wccTask).join();

        // 4. Aggregate Results
        ClientProfileDTO dto = new ClientProfileDTO();
        dto.setClient(client);

        try {
            List<Invoice> invoices = invoicesTask.get();
            List<WorkCompletionCertificate> wccs = wccTask.get();

            // Set Recent Invoices (Sorted by Date DESC, Limit 10)
            if (invoices != null) {
                invoices.sort(Comparator.comparing(Invoice::getIssuedAt).reversed());
                dto.setRecentInvoices(invoices.stream().limit(10).collect(Collectors.toList()));
            } else {
                dto.setRecentInvoices(Collections.emptyList());
                invoices = Collections.emptyList();
            }

            // Set WCCs
            dto.setRecentCertificates(wccs != null ? wccs : Collections.emptyList());

            // Calculate Stats
            double totalBilled = invoices.stream().mapToDouble(Invoice::getTotal).sum();
            
            // Calculate Pending
            double pending = invoices.stream()
                    .filter(inv -> "PENDING".equalsIgnoreCase(inv.getStatus()) || "UNPAID".equalsIgnoreCase(inv.getStatus()))
                    .mapToDouble(Invoice::getTotal)
                    .sum();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBilled", totalBilled);
            stats.put("pendingAmount", pending);
            stats.put("invoiceCount", invoices.size());
            stats.put("wccCount", wccs != null ? wccs.size() : 0);

            dto.setStats(stats);

        } catch (Exception e) {
            throw new RuntimeException("Error assembling client profile", e);
        }

        return dto;
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