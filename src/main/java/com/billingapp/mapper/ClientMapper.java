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
        
        // 👇 FIX 1: Map the Dates (so columns are not blank)
        dto.setCreatedAt(client.getCreatedAt());
        dto.setUpdatedAt(client.getUpdatedAt());

        // 👇 FIX 2: Map GSTIN (Entity has 'gstin', DTO has 'gstNo')
        dto.setGstin(client.getGstin());
        dto.setState(client.getState());
        dto.setStateCode(client.getStateCode());
        dto.setPincode(client.getPincode());
        
        return dto;
    }

    public Client toEntity(ClientDTO dto) {
        if (dto == null) return null;
        
        return Client.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .notes(dto.getNotes())
                // 👇 FIX 3: Map GST back to Entity
                .gstin(dto.getGstin())
                // Preserve dates if they exist (optional, usually handled by Service)
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .state(dto.getState())
                .stateCode(dto.getStateCode())
                .pincode(dto.getPincode())
                .build();
    }
}