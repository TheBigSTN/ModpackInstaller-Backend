package com.stelian.modpack_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModpackFileDTO {
    private String filePath;
    private String sha256;
    private long fileSize;
}
