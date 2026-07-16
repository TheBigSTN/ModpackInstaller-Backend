package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.ModpackVersionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModpackVersionFileRepository extends JpaRepository<ModpackVersionFile, UUID> {
    boolean existsByStoredFileSha256(String sha256);
}