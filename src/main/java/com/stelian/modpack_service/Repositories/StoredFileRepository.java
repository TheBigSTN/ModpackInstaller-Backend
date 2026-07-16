package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, String> {
}