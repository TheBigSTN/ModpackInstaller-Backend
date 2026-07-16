package com.stelian.modpack_service.DTOs;

import com.stelian.modpack_service.Entities.ModpackVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModpackVersionDTO {

    private UUID id;
    private String semver;
    private String versionName;
    private VersionStatus status;
    private String modpackId;
    private LocalDateTime createdAt;

    public static ModpackVersionDTO from(ModpackVersion version) {
        return ModpackVersionDTO.builder()
                .id(version.getId())
                .semver(version.getSemver())
                .versionName(version.getVersionName())
                .status(version.getStatus())
                .modpackId(version.getModpack().getId().toString())
                .createdAt(version.getCreatedAt())
                .build();
    }
}