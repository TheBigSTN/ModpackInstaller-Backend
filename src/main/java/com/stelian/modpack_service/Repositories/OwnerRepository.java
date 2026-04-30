package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.ModpackOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<ModpackOwner, String> {
}
