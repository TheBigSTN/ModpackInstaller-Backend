package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Entities.ModpackVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModpackVersionRepository extends JpaRepository<ModpackVersion, UUID> {
    Optional<ModpackVersion> findByModpackAndSemver(Modpack modpack, String semver); // Updated to use semver (String)
}