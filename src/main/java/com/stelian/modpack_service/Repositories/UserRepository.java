package com.stelian.modpack_service.Repositories;

import com.stelian.modpack_service.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByToken(String token);
}
