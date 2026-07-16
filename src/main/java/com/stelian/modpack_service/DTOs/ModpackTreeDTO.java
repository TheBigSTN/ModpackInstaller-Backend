package com.stelian.modpack_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModpackTreeDTO {
    private List<ModpackFileDTO> files;

}