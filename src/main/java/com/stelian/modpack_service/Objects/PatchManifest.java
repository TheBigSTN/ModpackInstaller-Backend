package com.stelian.modpack_service.Objects;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PatchManifest {

    private int schemaVersion = 1;

    private List<InstalledModInfo> added = new ArrayList<>();
    private List<InstalledModInfo> removed = new ArrayList<>();

    private boolean empty = false;

    public boolean isEmpty() {
        empty = added.isEmpty() && removed.isEmpty();
        return empty;
    }
}