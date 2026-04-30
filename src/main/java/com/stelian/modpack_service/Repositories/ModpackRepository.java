package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.Modpack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ModpackRepository extends JpaRepository<Modpack, String> {
    List<Modpack> findByOwnerOwnerToken(String ownerToken);

    List<Modpack> findByIsPublicTrue();
}