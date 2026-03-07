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
import lombok.extern.slf4j.Slf4j; // 👈 1. Added SLF4J Import
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

@Slf4j // 👈 2. Added SLF4J Annotation
@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final WorkCompletionCertificateRepository wccRepository;
    private final ClientMapper mapper;
    private final MongoTemplate mongoTemplate;

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
        log.info("Creating new client with name: {}", req.getName()); // 👈 Log creation start
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
        log.info("Client successfully created with ID: {}", saved.getId()); // 👈 Log success
        return mapper.toDto(saved);
    }

    @Override
    public ClientDTO update(String id, CreateClientRequest req) {
        log.info("Updating client profile for ID: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed: Client ID {} not found", id); // 👈 Log warning for bad IDs
                    return new IllegalArgumentException("Client not found: " + id);
                });
        
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
        log.info("Client ID: {} updated successfully", id);
        return mapper.toDto(saved);
    }

    @Override
    public ClientDTO getById(String id) {
        return clientRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.warn("Fetch failed: Client ID {} not found", id);
                    return new IllegalArgumentException("Client not found: " + id);
                });
    }

    @Override
    public ClientProfileDTO getClientProfile(String clientId) {
        log.info("Assembling profile for client: {}", clientId);
        long startTime = System.currentTimeMillis(); // 👈 Start timer

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.warn("Profile fetch failed: ID {} not found", clientId);
                    return new IllegalArgumentException("Client not found: " + clientId);
                });

        CompletableFuture<List<Invoice>> invoicesTask = CompletableFuture.supplyAsync(() -> 
            invoiceRepository.findByClientId(clientId)
        );

        CompletableFuture<List<WorkCompletionCertificate>> wccTask = CompletableFuture.supplyAsync(() -> {
            List<WorkCompletionCertificate> byId = wccRepository.findByClientId(clientId);
            if (byId != null && !byId.isEmpty()) {
                return byId;
            }
            log.info("No WCC found by ID for client {}, falling back to name-based lookup for: {}", clientId, client.getName());
            return wccRepository.findByStoreNameIgnoreCase(client.getName());
        });

        CompletableFuture.allOf(invoicesTask, wccTask).join();

        ClientProfileDTO dto = new ClientProfileDTO();
        dto.setClient(client);

        try {
            List<Invoice> invoices = invoicesTask.get();
            List<WorkCompletionCertificate> wccs = wccTask.get();

            if (invoices != null) {
                invoices.sort(Comparator.comparing(Invoice::getIssuedAt).reversed());
                dto.setRecentInvoices(invoices.stream().limit(10).collect(Collectors.toList()));
            } else {
                dto.setRecentInvoices(Collections.emptyList());
                invoices = Collections.emptyList();
            }

            dto.setRecentCertificates(wccs != null ? wccs : Collections.emptyList());

            double totalBilled = invoices.stream().mapToDouble(Invoice::getTotal).sum();
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
            
            long endTime = System.currentTimeMillis();
            log.info("Profile assembly for {} took {} ms", client.getName(), (endTime - startTime)); // 👈 Performance log

        } catch (Exception e) {
            log.error("Critical error assembling profile for client {}: {}", clientId, e.getMessage()); // 👈 Error log
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
        log.warn("Request received to DELETE client ID: {}", id); // 👈 Warn for destructive actions
        clientRepository.deleteById(id);
        log.info("Client ID: {} successfully deleted", id);
    }

    @Override
    public Page<ClientDTO> search(String q, int page, int size, String sort) {
        log.info("Client search initiated: query='{}', page={}, size={}", q, page, size);
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Sort s = parseSort(sort, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Query query = new Query(); 

        if (q != null && !q.isBlank()) {
            String regex = ".*" + q.trim() + ".*";
            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("name").regex(regex, "i"),
                    Criteria.where("email").regex(regex, "i")
            );
            query.addCriteria(criteria);
        }

        long total = mongoTemplate.count(query, Client.class);
        query.with(pageable);

        List<Client> list = mongoTemplate.find(query, Client.class);
        log.info("Search completed. Found {} total matching records", total);
        
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
            log.debug("Invalid sort parameter '{}', using default", sort);
            return defaultSort;
        }
    }
}