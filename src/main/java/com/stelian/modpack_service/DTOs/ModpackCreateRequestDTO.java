package com.stelian.modpack_service.DTOs;

import lombok.Data;

@Data
public class ModpackCreateRequestDTO {
    private String id;
    private String name;
    private String description;
    private String gameVersion;
    private ModLoaderType loader;
    private String loaderVersion;
    private String sharingCode; // Optional la creare
    private boolean isPublic;
}