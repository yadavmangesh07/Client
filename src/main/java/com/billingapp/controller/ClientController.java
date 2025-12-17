package com.billingapp.controller;

import com.billingapp.dto.ClientDTO;
import com.billingapp.dto.CreateClientRequest;
import com.billingapp.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody CreateClientRequest req) {
        ClientDTO dto = service.create(req);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> update(@PathVariable String id,
                                            @Valid @RequestBody CreateClientRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
public ResponseEntity<Page<ClientDTO>> searchClients(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "sort", required = false) String sort
) {
    Page<ClientDTO> result = service.search(q, page, size, sort);
    return ResponseEntity.ok(result);
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
