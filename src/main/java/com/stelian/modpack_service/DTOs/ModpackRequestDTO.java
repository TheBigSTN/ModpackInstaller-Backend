package com.stelian.modpack_service.DTOs;

import lombok.Data;

@Data
public class ModpackRequestDTO {
    private String name;
    private String description;
    private String gameVersion;
    private String loader; // Păstrăm String pentru simplitate în mapare
    private String loaderVersion;
    private String sharingCode;
    private boolean isPublic;
}