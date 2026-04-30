package com.stelian.modpack_service.Controllers;

import com.stelian.modpack_service.DTOs.ModpackCreateRequestDTO;
import com.stelian.modpack_service.DTOs.ModpackRequestDTO;
import com.stelian.modpack_service.DTOs.ModpackResponseDTO;
import com.stelian.modpack_service.DTOs.PublicModpackResponseDTO;
import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Entities.ModpackOwner;
import com.stelian.modpack_service.Services.ModpackService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/modpacks")
@RequiredArgsConstructor
public class ModpackController {

    private final ModpackService modpackService;

    @PostMapping("/register")
    public ResponseEntity<ModpackOwner> register(@RequestParam String nickname) {
        return ResponseEntity.ok(modpackService.registerNewOwner(nickname));
    }

    // 1. INIȚIALIZARE / CREARE MODPACK
    @PostMapping
    public ResponseEntity<?> createModpack(
            @RequestBody ModpackCreateRequestDTO dto,
            @RequestHeader(value = "X-Owner-Token") String ownerToken) {

        if (ownerToken == null || modpackService.validateOwner(ownerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Trebuie să te înregistrezi mai întâi (/register)");
        }

        ModpackResponseDTO created = modpackService.createInitialModpack(dto, ownerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my-library")
    public ResponseEntity<?> getMyModpacks(@RequestHeader("X-Owner-Token") String ownerToken) {
        if (modpackService.validateOwner(ownerToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalid.");
        }

        List<Modpack> list = modpackService.getModpacksByOwner(ownerToken);
        return ResponseEntity.ok(list);
    }

    // 2. UPDATE METADATA (Fără fișier)
    @PutMapping("/{modpackId}")
    public ResponseEntity<?> updateMetadata(
            @PathVariable String modpackId,
            @RequestBody ModpackRequestDTO dto,
            @RequestHeader("X-Owner-Token") String ownerToken,
            @RequestHeader("X-Modpack-Password") String password) {

        // Verificăm dacă cel care face PUT are dreptul să modifice acest modpack
        if (modpackService.authorizeModpackAction(modpackId, ownerToken, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acces neautorizat.");
        }

        return ResponseEntity.ok(modpackService.updateMetadata(modpackId, dto));
    }

    @GetMapping("/{modpackId}")
    public ResponseEntity<Modpack> getModpack(
            @PathVariable String modpackId,
            @RequestParam(required = false) String code) {
        var modpack = modpackService.getModpackIfAccessible(modpackId, code);
        modpack.setModpackPassword("");
        var owner = modpack.getOwner();
        owner.setOwnerToken("");

        modpack.setOwner(owner);
        return ResponseEntity.ok(modpack);
    }

    // 3. UPLOAD NEW VERSION (Doar fișierul și numărul versiunii)
    @PostMapping(value = "/{modpackId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVersion(
            @PathVariable String modpackId,
            @RequestParam("version") int version,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-Owner-Token") String ownerToken,
            @RequestHeader("X-Modpack-Password") String password) {

        if (!modpackService.authorizeModpackAction(modpackId, ownerToken, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token sau Parolă modpack incorectă.");
        }

        try {
            modpackService.addVersion(modpackId, version, file);
            return ResponseEntity.ok("Version uploaded.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    // 🔹 FULL VERSION DOWNLOAD (Securizat)
    @GetMapping("/{modpackId}/versions/{version}")
    public ResponseEntity<UrlResource> downloadVersion(
            @PathVariable String modpackId,
            @PathVariable int version,
            @RequestParam(required = false) String code) throws IOException {

        Modpack modpack = modpackService.getModpackIfAccessible(modpackId, code);
        Path zip = modpackService.getFullVersionZip(modpackId, version);
        return download(zip, modpack.getName() + "-v" + version + ".zip");
    }

    // 🔹 COMBINED PATCH DOWNLOAD (Securizat)
    @GetMapping("/{modpackId}/patch")
    public ResponseEntity<UrlResource> downloadPatch(
            @PathVariable String modpackId,
            @RequestParam int from,
            @RequestParam int to,
            @RequestParam(required = false) String code) throws IOException {

        Modpack modpack = modpackService.getModpackIfAccessible(modpackId, code);
        Path patch = modpackService.getCombinedPatch(modpackId, from, to);
        return download(patch, modpack.getName() + "-patch-" + from + "-to-" + to + ".zip");
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

        if (modpackService.authorizeModpackAction(id, ownerToken, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        modpackService.deleteModpack(id);
        return ResponseEntity.ok("Modpack deleted successfully.");
    }

    private ResponseEntity<UrlResource> download(Path path, String name) throws IOException {
        UrlResource res = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "\"")
                .body(res);
    }
}