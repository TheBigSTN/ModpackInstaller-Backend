package com.stelian.modpack_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LauncherFilesStatusDTO {
    private boolean hasTlauncherAdditional;
    private boolean hasTlauncherModpackJson;
}
