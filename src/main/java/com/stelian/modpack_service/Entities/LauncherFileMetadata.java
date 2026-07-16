package com.stelian.modpack_service.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "launcher_file_metadata")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LauncherFileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String loader;

    @Column(nullable = false)
    private String loaderVersion;

    @Column(nullable = false)
    private String gameVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LauncherFileType fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_sha256", referencedColumnName = "sha256", nullable = false)
    private StoredFile storedFile;

    // Removed originalModpackName as it will be provided by the client during download
}
