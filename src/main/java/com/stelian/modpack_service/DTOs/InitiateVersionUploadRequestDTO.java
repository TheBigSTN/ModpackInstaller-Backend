package com.stelian.modpack_service.DTOs;

import lombok.Data;

import java.util.UUID;

@Data
public class InitiateVersionUploadRequestDTO {
    private UUID modpackId;
    private String semver;
    private String versionName;
}