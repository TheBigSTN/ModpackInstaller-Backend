package com.stelian.modpack_service.DTOs;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID; // Import UUID

@Data
@Builder
@Getter
@Setter
public class PublicModpackResponseDTO {
    private String id;
    private String modpackName;
    private String authorName;
    private String gameVersion;
    private ModLoaderType loader;
    private String loaderVersion;
    private String latestVersion;
    private UUID latestVersionId; // New field for the UUID of the latest version
    private Instant createdAt;
    private Instant modifiedAt;
}