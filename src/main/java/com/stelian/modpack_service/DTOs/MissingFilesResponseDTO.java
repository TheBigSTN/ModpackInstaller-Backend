package com.stelian.modpack_service.DTOs;

import lombok.Data;
import java.util.List;

@Data
public class MissingFilesResponseDTO {
    private List<ModpackFileDTO> missingFiles;
}