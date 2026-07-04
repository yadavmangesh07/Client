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
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

@Slf4j
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
    @CacheEvict(value = "clients", allEntries = true) // Flushes master list cache entries on change
    public ClientDTO create(CreateClientRequest req) {
        log.info("Attempting to create Client profile record with name: {}", req.getName());
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
        log.info("Client tracking token successfully written to collection persistence layer with inner record ID: {}", saved.getId());
        return mapper.toDto(saved);
    }

    @Override
    @CacheEvict(value = "clients", allEntries = true) // Evicts old cache data on profile changes
    public ClientDTO update(String id, CreateClientRequest req) {
        log.info("Attempting to commit transaction modification delta configurations on Client reference ID: {}", id);
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Update payload configuration rejected: target mapping entity matching code ID {} doesn't exist", id);
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
        log.info("Client state modifications for target profile identity matching ID {} successfully persisted", id);
        return mapper.toDto(saved);
    }

    @Override
    @Cacheable(value = "clients", key = "#id") // Caches specific individual single data rows
    public ClientDTO getById(String id) {
        log.debug("Executing structured data mapping find lookup query for instance token key parameter ID: {}", id);
        return clientRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.error("Data lookup request failed: Client record signature ID {} is missing or non-existent", id);
                    return new IllegalArgumentException("Client not found: " + id);
                });
    }

    @Override
    @Cacheable(value = "clients", key = "'profile-' + #clientId") // Bypasses heavy multi-repo threads if loaded recently
    public ClientProfileDTO getClientProfile(String clientId) {
        log.info("Assembling dynamic complex metrics profile aggregation maps for client: {}", clientId);
        long startTime = System.currentTimeMillis();

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    log.error("Profile aggregate assembly aborted: Client context ID {} non-existent", clientId);
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
            log.info("No explicit WCC keys mapped by target ID for client {}; fallback matching execution executed on entity name: {}", clientId, client.getName());
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
            log.info("Asynchronous multi-thread profile block compilation for client [{}] finalized in {} ms", client.getName(), (endTime - startTime));

        } catch (Exception e) {
            log.error("Critical analytical execution failure compiling sub-repository streams for client reference " + clientId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error assembling client profile", e);
        }

        return dto;
    }

    @Override
    @Cacheable(value = "clients")
    public List<ClientDTO> getAll() {
        log.debug("Executing collective relational dump vector lookup query against Client transactional datasets");
        return clientRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "clients", allEntries = true) // Invalidates cache keys entirely upon deletion
    public void delete(String id) {
        log.warn("Initiating structural row cache eviction and physical deletion sequence for Client entity element key ID: {}", id);
        clientRepository.deleteById(id);
        log.info("Client structural row block reference ID: {} successfully purged from system persistence collections", id);
    }

    @Override
    public Page<ClientDTO> search(String q, int page, int size, String sort) {
        log.debug("Client search index initialized with query parameters: string='{}', pageIndex={}, limitSize={}", q, page, size);
        
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
        log.info("Regex search iteration query completed. Found {} total matching criteria row document records", total);
        
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
            log.debug("Gracefully handling sort evaluation format failure for configuration key string '{}'; reverting data array alignment to default parameters", sort);
            return defaultSort;
        }
    }
}