package com.stelian.modpack_service.DTOs;

import lombok.Data;

import java.util.UUID; // Import UUID

@Data
public class UpdateVersionStatusDTO {
    private UUID modpackId;
    private UUID versionId;
    private VersionStatus newStatus;
}