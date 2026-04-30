package com.stelian.modpack_service.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private int versionNumber;
    private String zipPath;
    private String patchPath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modpack_id")
    @JsonBackReference
    private Modpack modpack;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}