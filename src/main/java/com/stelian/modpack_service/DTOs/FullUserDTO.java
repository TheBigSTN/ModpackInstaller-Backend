package com.stelian.modpack_service.DTOs;

import com.stelian.modpack_service.Entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record FullUserDTO(
        UUID id,
        String token,
        String username,
        LocalDateTime createdAt
) {
    public static FullUserDTO from(User owner) {
        return new FullUserDTO(
                owner.getId(),
                owner.getToken(),
                owner.getUsername(),
                owner.getCreatedAt()
        );
    }
}
