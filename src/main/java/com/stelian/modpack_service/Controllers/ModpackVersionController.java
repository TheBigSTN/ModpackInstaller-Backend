package com.stelian.modpack_service.Controllers;

import com.stelian.modpack_service.DTOs.*;
import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Entities.ModpackVersion;
import com.stelian.modpack_service.Services.ModpackService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/modpacks/{modpackId}/version")
@RequiredArgsConstructor
public class ModpackVersionController {

    private final ModpackService modpackService;

    // New endpoint: Initiate Version Upload
    @PostMapping("/initiate")
    public ResponseEntity<ModpackVersionDTO> initiateVersionUpload(
            @PathVariable UUID modpackId,
            @RequestBody InitiateVersionUploadRequestDTO dto,
            @RequestHeader("X-Owner-Token") String ownerToken) {
        if (!modpackId.equals(dto.getModpackId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modpack ID in path and body must match.");

        ModpackVersion newVersion = modpackService.initiateVersionUpload(dto, ownerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(ModpackVersionDTO.from(newVersion));
    }

    // New endpoint: Upload Tree JSON
    @PostMapping("/{versionId}/tree")
    public ResponseEntity<MissingFilesResponseDTO> uploadTreeJson(
            @PathVariable UUID versionId,
            @RequestBody ModpackTreeDTO treeDto,
            @RequestHeader("X-Owner-Token") String ownerToken) {
        MissingFilesResponseDTO response = modpackService.uploadTreeJson(versionId, treeDto, ownerToken);
        return ResponseEntity.ok(response);
    }

    // New endpoint: Update Version Status
    @PutMapping("/{versionId}/status")
    public ResponseEntity<?> updateVersionStatus(
            @PathVariable UUID modpackId,
            @PathVariable UUID versionId,
            @RequestBody UpdateVersionStatusDTO dto,
            @RequestHeader("X-Owner-Token") String ownerToken) {
        // modpackId from path variable should match the one in DTO
        if (!modpackId.equals(dto.getModpackId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modpack ID in path and body must match.");
        }
        // versionId from path variable should match the one in DTO
        if (!versionId.equals(dto.getVersionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version ID in path and body must match.");
        }
        modpackService.updateVersionStatus(modpackId, versionId, dto.getNewStatus(), ownerToken);
        return ResponseEntity.ok("Version status updated successfully.");
    }

    // New endpoint: Get Modpack Version Tree
    @GetMapping("/{versionId}/tree")
    public ResponseEntity<ModpackTreeDTO> getModpackVersionTree(
            @PathVariable UUID versionId) {
        ModpackTreeDTO tree = modpackService.getModpackVersionTree(versionId);
        return ResponseEntity.ok(tree);
    }

    // New endpoint: Get all versions for a modpack
    @GetMapping
    public ResponseEntity<List<ModpackVersionDTO>> getModpackVersions(
            @PathVariable UUID modpackId,
            @RequestParam(required = false) String code,
            @RequestHeader(value = "X-Owner-Token", required = false) String ownerToken) {
        List<ModpackVersionDTO> versions = modpackService.getModpackVersions(modpackId.toString(), code, ownerToken);
        return ResponseEntity.ok(versions);
    }

    // Get the modpack manifest of a version
    @GetMapping("/{versionId}/manifest")
    public ResponseEntity<InputStreamResource> getModpackManifest(
            @PathVariable UUID versionId,
            @RequestParam(required = false) String code) throws IOException {
        Path manifestPath = modpackService.getModpackManifestFile(versionId, code);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(manifestPath));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(Files.size(manifestPath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"manifest.json\"")
                .body(resource);
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<UrlResource> downloadVersion(
            @PathVariable String modpackId,
            @PathVariable UUID versionId,
            @RequestParam(required = false) String code) throws IOException {

        Modpack modpack = modpackService.getModpackIfAccessible(UUID.fromString(modpackId), code); // Authorization check
        Path zip = modpackService.getFullVersionZip(modpackId, versionId);
        // Fetch the version to get its semver for the filename
        ModpackVersion version = modpackService.getVersionById(versionId);
        return download(zip, modpack.getName() + "-v" + version.getSemver() + ".zip");
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<?> deleteModpackVersion(
            @PathVariable String modpackId,
            @PathVariable UUID versionId,
            @RequestHeader("X-Owner-Token") String ownerToken) {
        modpackService.deleteModpackVersion(modpackId, versionId, ownerToken);
        return ResponseEntity.ok("Modpack version deleted successfully.");
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