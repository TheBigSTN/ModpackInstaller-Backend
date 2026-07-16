package com.stelian.modpack_service.Entities;

import com.stelian.modpack_service.DTOs.StoredFileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "stored_files")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StoredFile {
    @Id
    private String sha256; // SHA-256 hash as primary key

    private long size; // Size of the file in bytes

    @Enumerated(EnumType.STRING)
    private StoredFileStatus status; // PENDING or UPLOADED

    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}