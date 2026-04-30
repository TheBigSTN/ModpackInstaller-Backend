package com.stelian.modpack_service.DTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModpackMetadataDTO {
    private String id;
    private String name;
    private int version;
    private String ownerNickname;
    private boolean isPublic;
    private String sharingCode;
    private String gameVersion;
    private ModLoaderType loader;
    private String loaderVersion;
    private String author;
    private String description;
    private String installPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
