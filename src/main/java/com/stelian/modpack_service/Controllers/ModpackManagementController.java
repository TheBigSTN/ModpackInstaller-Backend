package com.stelian.modpack_service.Controllers;

import com.stelian.modpack_service.DTOs.*;
import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Services.ModpackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/modpacks")
@RequiredArgsConstructor
public class ModpackManagementController {

    private final ModpackService modpackService;

    @PostMapping("/register")
    public ResponseEntity<FullUserDTO> register(@RequestParam String username) {
        var user = modpackService.registerNewOwner(username);

        return ResponseEntity.ok(FullUserDTO.from(user));
    }

    @PostMapping
    public ResponseEntity<?> createModpack(
            @RequestBody ModpackCreateRequestDTO dto,
            @RequestHeader(value = "X-Owner-Token") String ownerToken) {

        if (modpackService.validateOwner(ownerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Trebuie să te înregistrezi mai întâi (/register)");
        }

        Modpack modpack = modpackService.createInitialModpack(dto, ownerToken);
        ModpackDTO authorisedDto = ModpackDTO.fromAuthorized(modpack);

        return ResponseEntity.status(HttpStatus.CREATED).body(authorisedDto);
    }

    @GetMapping("/my-library")
    public ResponseEntity<?> getMyModpacks(@RequestHeader("X-Owner-Token") String ownerToken) {
        if (modpackService.validateOwner(ownerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalid.");
        }

        List<Modpack> list = modpackService.getModpacksByOwner(ownerToken);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{modpackId}")
    public ResponseEntity<?> updateMetadata(
            @PathVariable String modpackId,
            @RequestBody ModpackRequestDTO dto,
            @RequestHeader("X-Owner-Token") String ownerToken,
            @RequestHeader("X-Modpack-Password") String password) {

        if (modpackService.authorizeModpackAction(UUID.fromString(modpackId), ownerToken, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acces neautorizat.");
        }

        return ResponseEntity.ok(modpackService.updateMetadata(modpackId, dto));
    }

    @GetMapping("/{modpackId}")
    public ResponseEntity<ModpackDTO> getModpack(
            @PathVariable UUID modpackId,
            @RequestParam(required = false) String code) {

        Modpack modpack = modpackService.getModpackIfAccessible(modpackId, code);

        ModpackDTO response = ModpackDTO.from(modpack);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<List<PublicModpackResponseDTO>> getPublicModpacks() {
        return ResponseEntity.ok(modpackService.findAllPublic());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteModpack(
            @PathVariable String id,
            @RequestHeader("X-Owner-Token") String ownerToken,
            @RequestHeader("X-Modpack-Password") String password) {

        modpackService.deleteModpack(id, ownerToken, password);
        return ResponseEntity.ok("Modpack deleted successfully.");
    }
}
