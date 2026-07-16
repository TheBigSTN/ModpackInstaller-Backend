package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.Modpack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModpackRepository extends JpaRepository<Modpack, UUID> {
    List<Modpack> findByOwnerToken(String token);

    List<Modpack> findByIsPublicTrue();
}