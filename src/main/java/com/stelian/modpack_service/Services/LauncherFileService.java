package com.stelian.modpack_service.Services;

import com.stelian.modpack_service.DTOs.LauncherFileDTO;
import com.stelian.modpack_service.DTOs.LauncherFilesStatusDTO;
import com.stelian.modpack_service.DTOs.StoredFileStatus;
import com.stelian.modpack_service.Entities.LauncherFileMetadata;
import com.stelian.modpack_service.Entities.LauncherFileType;
import com.stelian.modpack_service.Entities.StoredFile;
import com.stelian.modpack_service.Repositories.LauncherFileMetadataRepository;
import com.stelian.modpack_service.Repositories.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LauncherFileService {

    private final LauncherFileMetadataRepository launcherFileMetadataRepo;
    private final StoredFileRepository storedFileRepo;
    // Removed ModpackService dependency as it's no longer needed for owner validation

    private Path storageRoot;

    // Removed MODPACK_NAME_PLACEHOLDER constant as file processing is commented out for upload
    // private static final String MODPACK_NAME_PLACEHOLDER = "__MODPACK_NAME_PLACEHOLDER__";

    @Value("${app.storage.root-path}")
    private void setStorageRoot(String path) {
        this.storageRoot = Paths.get(path);
    }

    @Transactional
    public void uploadLauncherFile(String loader, String loaderVersion, String gameVersion, LauncherFileType fileType, MultipartFile file) throws IOException { // Removed ownerToken and modpackName
        // Removed owner validation as per request

        Optional<LauncherFileMetadata> existingMetadataOpt = launcherFileMetadataRepo.findByLoaderAndLoaderVersionAndGameVersionAndFileType(
                loader, loaderVersion, gameVersion, fileType);

        if (existingMetadataOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Launcher file of type " + fileType +
                    " for loader " + loader + ", loaderVersion " + loaderVersion + ", gameVersion " + gameVersion + " already exists.");
        }

        // Store original content directly without processing for now
        String originalContent = new String(file.getBytes());
        String sha256 = calculateSha256(new ByteArrayInputStream(originalContent.getBytes()));

        StoredFile storedFile = storeFileContent(sha256, originalContent.getBytes());

        LauncherFileMetadata metadata = LauncherFileMetadata.builder()
                .loader(loader)
                .loaderVersion(loaderVersion)
                .gameVersion(gameVersion)
                .fileType(fileType)
                .storedFile(storedFile)
                .build();
        launcherFileMetadataRepo.save(metadata);
    }

    /*
    // Commented out file processing logic for future use
    private static @NonNull String getContentToStore(LauncherFileType fileType, MultipartFile file, String modpackName) throws IOException {
        String originalContent = new String(file.getBytes());
        String contentToStore = originalContent;

        if (fileType == LauncherFileType.TlauncherModpackJson) {
            // For TlauncherModpackJson, we replace the actual modpack name in the uploaded file with a placeholder
            // The actual modpack name will be provided at download time
            if (modpackName == null || modpackName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "modpackName is required for TlauncherModpackJson file type.");
            }
            contentToStore = originalContent.replace(modpackName, MODPACK_NAME_PLACEHOLDER);
        }
        return contentToStore;
    }
    */

    public LauncherFilesStatusDTO getLauncherFilesStatus(String loader, String loaderVersion, String gameVersion) {
        boolean hasTlauncherAdditional = launcherFileMetadataRepo.findByLoaderAndLoaderVersionAndGameVersionAndFileType(
                loader, loaderVersion, gameVersion, LauncherFileType.TlauncherAdditional).isPresent();

        boolean hasTlauncherModpackJson = launcherFileMetadataRepo.findByLoaderAndLoaderVersionAndGameVersionAndFileType(
                loader, loaderVersion, gameVersion, LauncherFileType.TlauncherModpackJson).isPresent();

        return new LauncherFilesStatusDTO(hasTlauncherAdditional, hasTlauncherModpackJson);
    }

    public LauncherFileDTO downloadLauncherFile(String loader, String loaderVersion, String gameVersion, LauncherFileType fileType, String modpackName) throws IOException {
        LauncherFileMetadata metadata = launcherFileMetadataRepo.findByLoaderAndLoaderVersionAndGameVersionAndFileType(
                        loader, loaderVersion, gameVersion, fileType)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Launcher file of type " + fileType +
                        " for loader " + loader + ", loaderVersion " + loaderVersion + ", gameVersion " + gameVersion + " not found."));

        StoredFile storedFile = metadata.getStoredFile();

        if (storedFile.getStatus() != StoredFileStatus.Uploaded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Launcher file is not yet uploaded.");
        }

        Path sourceFile = getStoredFilePath(storedFile.getSha256());
        if (!Files.exists(sourceFile)) {
            throw new IOException("Stored launcher file not found on disk: " + storedFile.getSha256());
        }

        String fileContent = Files.readString(sourceFile);

        // Placeholder replacement logic for download remains, as the client provides the modpackName
        if (fileType == LauncherFileType.TlauncherModpackJson) {
            // Assuming the placeholder is still present from a previous upload or will be manually inserted
            // If the file was uploaded without placeholder replacement, this will simply not find/replace anything.
            fileContent = fileContent.replace("__MODPACK_NAME_PLACEHOLDER__", modpackName);
        }

        String filename;
        if (fileType == LauncherFileType.TlauncherAdditional) {
            filename = "TlauncerAdditional.json";
        } else { // TlauncherModpackJson
            filename = modpackName + ".json"; // Use the provided modpackName for the filename
        }

        return new LauncherFileDTO(filename, fileContent);
    }

    private Path getStoredFilePath(String sha256) {
        String firstTwoChars = sha256.substring(0, 2);
        return storageRoot.resolve("files").resolve(firstTwoChars).resolve(sha256);
    }

    private StoredFile storeFileContent(String sha256, byte[] content) throws IOException {
        Path blobStorageDir = storageRoot
                .resolve("files")
                .resolve(sha256.substring(0, 2));

        Files.createDirectories(blobStorageDir);
        Path targetPath = blobStorageDir.resolve(sha256);

        Files.write(targetPath, content);

        StoredFile storedFile = storedFileRepo.findById(sha256).orElseGet(() -> StoredFile.builder()
                .sha256(sha256)
                .size((long) content.length)
                .status(StoredFileStatus.Pending)
                .createdAt(Instant.now())
                .build());
        storedFile.setStatus(StoredFileStatus.Uploaded);
        storedFile.setSize((long) content.length);
        return storedFileRepo.save(storedFile);
    }

    private String calculateSha256(InputStream is) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashedBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not found.", e);
        }
    }
}
