package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.stelian.modpack_service.DTOs.ModLoaderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "modpacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modpack {
    @Id
    private String id;
    private String name;
    private String ownerNickname;

    @Column(nullable = false)
    private String modpackPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_token", nullable = false)
    @JsonManagedReference
    private ModpackOwner owner;

    private String sharingCode;
    private boolean isPublic;
    private int latestVersion;

    // Proprietăți noi sincronizate cu DTO-ul
    private String gameVersion;

    @Enumerated(EnumType.STRING)
    private ModLoaderType loader; // Stocat ca String pentru simplitate în DB
    private String loaderVersion;
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String installPath;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "modpack", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    @Builder.Default
    @JsonManagedReference
    private List<ModpackVersion> versions = new ArrayList<>();
}