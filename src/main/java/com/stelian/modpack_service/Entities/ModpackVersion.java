package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference; // Import JsonManagedReference
import com.stelian.modpack_service.DTOs.VersionStatus; // Import the new enum
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modpack_versions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModpackVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // PostgreSQL suportă nativ UUID
    private UUID id;

    private String semver; // Changed from int versionNumber to String semver
    private String versionName; // New field for a human-readable version name

    @Enumerated(EnumType.STRING) // Store enum as String
    private VersionStatus status; // New field for version status

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modpack_id")
    @JsonBackReference
    private Modpack modpack;

    @OneToMany(mappedBy = "modpackVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<ModpackVersionFile> files = new ArrayList<>(); // New field for files

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}