package com.stelian.modpack_service.Objects;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModpackManifest {
    public List<InstalledModInfo> InstalledMods = new ArrayList<>();
}