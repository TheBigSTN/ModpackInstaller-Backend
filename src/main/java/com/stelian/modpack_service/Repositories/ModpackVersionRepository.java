package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Entities.ModpackVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModpackVersionRepository extends JpaRepository<ModpackVersion, UUID> {
    Optional<ModpackVersion> findByModpackAndVersionNumber(Modpack modpack, int versionNumber);
}