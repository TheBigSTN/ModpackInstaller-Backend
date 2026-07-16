package com.stelian.modpack_service.Controllers;

import com.stelian.modpack_service.DTOs.LauncherFileDTO;
import com.stelian.modpack_service.DTOs.LauncherFilesStatusDTO;
import com.stelian.modpack_service.Entities.LauncherFileType;
import com.stelian.modpack_service.Services.LauncherFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/launcher-files")
@RequiredArgsConstructor
public class LauncherFilesController {

    private final LauncherFileService launcherFileService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadLauncherFile(
            @RequestParam String loader,
            @RequestParam String loaderVersion,
            @RequestParam String gameVersion,
            @RequestParam LauncherFileType fileType,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String modpackName) throws IOException {

        launcherFileService.uploadLauncherFile(loader, loaderVersion, gameVersion, fileType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body("Launcher file uploaded successfully.");
    }

    @GetMapping("/status")
    public ResponseEntity<LauncherFilesStatusDTO> getLauncherFilesStatus(
            @RequestParam String loader,
            @RequestParam String loaderVersion,
            @RequestParam String gameVersion) {
        LauncherFilesStatusDTO status = launcherFileService.getLauncherFilesStatus(loader, loaderVersion, gameVersion);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/download")
    public ResponseEntity<String> downloadLauncherFile(
            @RequestParam String loader,
            @RequestParam String loaderVersion,
            @RequestParam String gameVersion,
            @RequestParam LauncherFileType fileType,
            @RequestParam String modpackName) throws IOException {
        LauncherFileDTO fileDto = launcherFileService.downloadLauncherFile(loader, loaderVersion, gameVersion, fileType, modpackName);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDto.getFilePath() + "\"")
                .body(fileDto.getContent());
    }
}
