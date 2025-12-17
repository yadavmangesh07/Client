package com.billingapp.mapper;

import com.billingapp.dto.ClientDTO;
import com.billingapp.entity.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientDTO toDto(Client client) {
        if (client == null) return null;
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setName(client.getName());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setAddress(client.getAddress());
        dto.setNotes(client.getNotes());
        return dto;
    }

    public Client toEntity(ClientDTO dto) {
        if (dto == null) return null;
        Client client = Client.builder().build();
        client.setId(dto.getId());
        client.setName(dto.getName());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setAddress(dto.getAddress());
        client.setNotes(dto.getNotes());
        return client;
    }
}
