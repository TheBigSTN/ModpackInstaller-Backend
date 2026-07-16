package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.stelian.modpack_service.DTOs.ModLoaderType;
import com.stelian.modpack_service.DTOs.VersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modpacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modpack {
    @Id
    private UUID id;
    private String name;

    @Column(nullable = false)
    private String modpackPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonManagedReference
    private User owner;

    private String sharingCode;
    private boolean isPublic;

    @Transient
    @JsonIgnore
    public ModpackVersion getLatestVersion() {
        return versions.stream()
                .filter(v -> v.getStatus() == VersionStatus.Release)
                .max(Comparator.comparing(ModpackVersion::getCreatedAt))
                .orElse(null);
    }

    // Proprietăți noi sincronizate cu DTO-ul
    private String gameVersion;

    @Enumerated(EnumType.STRING)
    private ModLoaderType loader; // Stocat ca String pentru simplitate în DB
    private String loaderVersion;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "modpack", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("semver DESC")
    @Builder.Default
    @JsonManagedReference
    private List<ModpackVersion> versions = new ArrayList<>();
}