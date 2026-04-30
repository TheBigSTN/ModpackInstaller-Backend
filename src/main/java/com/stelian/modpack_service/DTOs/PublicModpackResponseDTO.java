package com.stelian.modpack_service.DTOs;


import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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
    private int latestVersion;
    private Instant createdAt;
    private Instant modifiedAt;
}
