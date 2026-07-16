package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.LauncherFileMetadata; // Changed import
import com.stelian.modpack_service.Entities.LauncherFileType; // Changed import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LauncherFileMetadataRepository extends JpaRepository<LauncherFileMetadata, UUID> { // Changed interface name and generic type
    Optional<LauncherFileMetadata> findByLoaderAndLoaderVersionAndGameVersionAndFileType(
            String loader, String loaderVersion, String gameVersion, LauncherFileType fileType); // Changed method signature
}
