package com.stelian.modpack_service.DTOs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModpackResponseDTO {
    private String id;
    private String name;
    private String ownerNickname;
    private String modpackPassword;
    private String ownerToken;
    private String author;
    private String gameVersion;
    private String loader;
    // Adaugă restul câmpurilor metadata aici, dar NU adăuga obiectul Owner complet
}
