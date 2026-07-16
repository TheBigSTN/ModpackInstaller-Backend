package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "modpack_version_files")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ModpackVersionFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @JsonBackReference
    private ModpackVersion modpackVersion;

    @Column(nullable = false)
    private String filePath; // Relative path within the modpack

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_sha256", referencedColumnName = "sha256", nullable = false)
    private StoredFile storedFile; // Reference to the actual stored file blob
}