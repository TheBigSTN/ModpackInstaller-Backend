package com.stelian.modpack_service.DTOs;

import com.stelian.modpack_service.Entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        LocalDateTime createdAt
) {
    public static UserDTO from(User owner) {
        return new UserDTO(
                owner.getId(),
                owner.getUsername(),
                owner.getCreatedAt()
        );
    }
}

