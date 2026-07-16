package com.stelian.modpack_service.Controllers;

import com.stelian.modpack_service.Services.ModpackService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final ModpackService modpackService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(
            @RequestPart("file") MultipartFile file) {
        try {
            String sha256 = modpackService.uploadFile(file);

            return ResponseEntity.ok(sha256);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error uploading file", e);
        }
    }

    @GetMapping("/{sha256}")
    public ResponseEntity<UrlResource> downloadStoredFile(
            @PathVariable String sha256) throws IOException {
        Path file = modpackService.downloadStoredFile(sha256);
        return download(file, sha256);
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
