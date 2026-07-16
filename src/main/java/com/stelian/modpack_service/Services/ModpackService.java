package com.stelian.modpack_service.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stelian.modpack_service.DTOs.*;
import com.stelian.modpack_service.Entities.*;
import com.stelian.modpack_service.Repositories.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ModpackService {

    private final ModpackRepository modpackRepo;
    private final ModpackVersionRepository versionRepo;
    private final ModpackVersionFileRepository versionFileRepo;
    private final UserRepository ownerRepo;
    private final StoredFileRepository storedFileRepo;

    private Path storageRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.storage.root-path}")
    private void setStorageRoot(String path) {
        this.storageRoot = Paths.get(path);
    }


    @Transactional
    public User registerNewOwner(String username) {
        String token = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User owner = User.builder()
                .token(token)
                .username(username)
                .createdAt(LocalDateTime.now())
                .build();
        return ownerRepo.save(owner);
    }

    public List<Modpack> getModpacksByOwner(String ownerToken) {
        return modpackRepo.findByOwnerToken(ownerToken);
    }

    public List<PublicModpackResponseDTO> findAllPublic() {
        return modpackRepo.findByIsPublicTrue().stream()
                .map(m -> {
                    return PublicModpackResponseDTO.builder()
                            .id(m.getId().toString())
                            .modpackName(m.getName())
                            .authorName(m.getName())
                            .gameVersion(m.getGameVersion())
                            .loader(m.getLoader())
                            .loaderVersion(m.getLoaderVersion())
                            .latestVersion(m.getLatestVersion().getSemver())
                            .latestVersionId(m.getLatestVersion().getId())
                            .createdAt(m.getCreatedAt())
                            .modifiedAt(m.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public Modpack createInitialModpack(ModpackCreateRequestDTO dto, String ownerToken) {
        // 1. Validare Owner (Folosim nickname-ul din contul real pentru Author/OwnerNickname)
        User owner = ownerRepo.findByToken(ownerToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Owner Token"));

        // 3. Generare Parolă Modpack
        String modpackPass = "pwd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 4. Creare și Configurare Entitate
        Modpack modpack = new Modpack();
        modpack.setId(dto.getId());
        modpack.setOwner(owner);
        modpack.setModpackPassword(modpackPass);

        // Setăm datele din DTO cu guardrails
        modpack.setName(dto.getName());
        modpack.setDescription(dto.getDescription());
        modpack.setGameVersion(dto.getGameVersion());
        modpack.setLoader(dto.getLoader() != null ? dto.getLoader() : null);
        modpack.setLoaderVersion(dto.getLoaderVersion());
        modpack.setPublic(dto.isPublic());

        // Validare sharingCode dacă este trimis la creare
        if (dto.getSharingCode() != null && !dto.getSharingCode().isEmpty()) {
            if (dto.getSharingCode().length() < 4 || dto.getSharingCode().length() > 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sharing code must be between 4 and 10 characters.");
            }
            modpack.setSharingCode(dto.getSharingCode());
        } else {
            modpack.setSharingCode(""); // Default empty
        }

        modpack.setCreatedAt(Instant.now());
        modpack.setUpdatedAt(Instant.now());

        return modpackRepo.save(modpack);
    }

    public boolean validateOwner(String token) {
        return ownerRepo.findByToken(token).isPresent();
    }

    @Transactional
    public void deleteModpack(String id, String ownerToken, String modpackPassword) {
        Modpack modpack = findModpackById(id);

        if (authorizeModpackAction(UUID.fromString(id), ownerToken, modpackPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to delete this modpack.");
        }

        Set<String> sha256sToClean = new HashSet<>();
        for (ModpackVersion version : modpack.getVersions()) {
            for (ModpackVersionFile file : version.getFiles()) {
                sha256sToClean.add(file.getStoredFile().getSha256());
            }
        }

       modpackRepo.delete(modpack);

        cleanupUnreferencedStoredFiles(sha256sToClean);
    }

    private @NonNull Modpack findModpackById(UUID id) {
        return modpackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modpack not found"));
    }

    private @NonNull Modpack findModpackById(String id) {
        return modpackRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modpack not found"));
    }

    @Transactional
    public void deleteModpackVersion(String modpackId, UUID versionId, String ownerToken) {
        Modpack modpack = findModpackById(modpackId);
        ModpackVersion version = findVersionById(versionId);

        // Ensure the version belongs to the modpack
        if (!version.getModpack().getId().equals(modpackId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to the specified modpack.");
        }

        // Authorize owner
        if (!modpack.getOwner().getToken().equals(ownerToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this modpack.");
        }

        // Collect all SHA256s from files of this specific version
        Set<String> sha256sToClean = new HashSet<>();
        for (ModpackVersionFile file : version.getFiles()) {
            sha256sToClean.add(file.getStoredFile().getSha256());
        }

        // Delete the ModpackVersion entity (this should cascade delete ModpackVersionFile entities)
        versionRepo.delete(version);

        // Clean up unreferenced stored files
        cleanupUnreferencedStoredFiles(sha256sToClean);
    }

    private @NonNull ModpackVersion findVersionById(UUID versionId) {
        return versionRepo.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modpack version not found"));
    }

    private void cleanupUnreferencedStoredFiles(Collection<String> sha256s) {
        for (String sha256 : sha256s) {
            if (!versionFileRepo.existsByStoredFileSha256(sha256)) {
                // No ModpackVersionFile references this StoredFile anymore
                storedFileRepo.findById(sha256).ifPresent(storedFile -> {
                    try {
                        Path filePath = getStoredFilePath(sha256);
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                        storedFileRepo.delete(storedFile);
                    } catch (IOException e) {
                        System.err.println("Failed to delete stored file " + sha256 + ": " + e.getMessage());
                        // Log the error, but continue with other files
                    }
                });
            }
        }
    }

    @Transactional
    public ModpackDTO updateMetadata(String id, ModpackRequestDTO dto) {
        Modpack modpack = findModpackById(id);

        // 1. Guardrail: Name (nu poate fi null sau gol)
        if (dto.getName() == null || dto.getName().trim().length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numele modpack-ului este prea scurt (min 3 ch).");
        }
        modpack.setName(dto.getName());

        // 2. Guardrail: Sharing Code
        String newCode = dto.getSharingCode();
        String currentCode = modpack.getSharingCode();

        // Verificăm dacă se încearcă schimbarea unui cod deja existent
        if (currentCode != null && !currentCode.isEmpty() && !currentCode.equals(newCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sharing code-ul nu mai poate fi schimbat odată ce a fost setat.");
        }

        // Verificăm lungimea codului nou (doar dacă se setează acum)
        if ((currentCode == null || currentCode.isEmpty()) && newCode != null && !newCode.isEmpty()) {
            if (newCode.length() < 4 || newCode.length() > 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sharing code-ul trebuie să aibă între 4 și 10 caractere.");
            }
            modpack.setSharingCode(newCode);
        }

        // 3. Guardrail: Câmpuri imuabile (Owner Nickname & Author)
        // NU preluăm valorile din DTO. Păstrăm ce avem deja în DB (că au fost setate la creare din Account)
        // modpack.setOwnerNickname(modpack.getOwner().getNickname()); // Opțional: forțăm sincronizarea cu contul

        // 4. Actualizăm restul câmpurilor permise
        modpack.setPublic(dto.isPublic());
        modpack.setGameVersion(dto.getGameVersion());
        modpack.setLoader(dto.getLoader() != null ? ModLoaderType.valueOf(dto.getLoader()) : null);
        modpack.setLoaderVersion(dto.getLoaderVersion());
        modpack.setDescription(dto.getDescription());

        // updatedAt se schimbă automat la orice modificare
        modpack.setUpdatedAt(Instant.now());

        Modpack saved = modpackRepo.save(modpack);
        return ModpackDTO.from(saved); // Returnăm DTO-ul curat, nu entitatea
    }

    @Transactional
    public ModpackVersion initiateVersionUpload(InitiateVersionUploadRequestDTO dto, String ownerToken) {
        Modpack modpack = findModpackById(dto.getModpackId());

        // Authorize owner
        if (!modpack.getOwner().getToken().equals(ownerToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this modpack.");
        }

        // Check if SemVer already exists for this modpack
        if (versionRepo.findByModpackAndSemver(modpack, dto.getSemver()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version " + dto.getSemver() + " already exists for this modpack.");
        }

        // Create a new ModpackVersion in DRAFT status
        ModpackVersion newVersion = ModpackVersion.builder()
                .modpack(modpack)
                .semver(dto.getSemver())
                .versionName(dto.getVersionName())
                .status(VersionStatus.Draft)
                .createdAt(LocalDateTime.now())
                .build();

        modpackRepo.save(modpack);
        return versionRepo.save(newVersion);
    }

    @Transactional
    public MissingFilesResponseDTO uploadTreeJson(UUID versionId, ModpackTreeDTO treeDto, String ownerToken) {
        ModpackVersion version = findVersionById(versionId);

        if (!version.getModpack().getOwner().getToken().equals(ownerToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this modpack.");
        }

        if (version.getStatus() != VersionStatus.Draft) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modpack version is not in DRAFT status. Cannot upload tree.json.");
        }

        List<ModpackFileDTO> missingFiles = new ArrayList<>();
        List<ModpackVersionFile> newVersionFiles = new ArrayList<>();

        for (ModpackFileDTO incomingFile : treeDto.getFiles()) {
            Optional<StoredFile> existingStoredFileOpt = storedFileRepo.findById(incomingFile.getSha256());
            StoredFile storedFile;

            if (existingStoredFileOpt.isEmpty() || existingStoredFileOpt.get().getStatus() == StoredFileStatus.Pending) {
                missingFiles.add(incomingFile);

                storedFile = existingStoredFileOpt.orElseGet(() -> StoredFile.builder()
                        .sha256(incomingFile.getSha256())
                        .size(incomingFile.getFileSize())
                        .status(StoredFileStatus.Pending)
                        .createdAt(Instant.now())
                        .build());
                storedFileRepo.save(storedFile);
            } else {
                storedFile = existingStoredFileOpt.get();
            }

            newVersionFiles.add(ModpackVersionFile.builder()
                    .modpackVersion(version)
                    .filePath(incomingFile.getFilePath())
                    .storedFile(storedFile)
                    .build());
        }

        version.getFiles().clear();
        version.getFiles().addAll(newVersionFiles);
        versionRepo.save(version);

        MissingFilesResponseDTO response = new MissingFilesResponseDTO();
        response.setMissingFiles(missingFiles);
        return response;
    }

    @Transactional
    public String uploadFile(MultipartFile file) throws IOException {
        String sha256 = calculateSha256(file.getInputStream());

        // 3. Check if file already exists and is uploaded
        Optional<StoredFile> existingStoredFileOpt = storedFileRepo.findById(sha256);
        if (existingStoredFileOpt.isPresent() && existingStoredFileOpt.get().getStatus() == StoredFileStatus.Uploaded) {
            return sha256; // File already uploaded, return its SHA256
        }

        // 4. Construct the storage path (blob storage)
        String firstTwoChars = sha256.substring(0, 2);
        Path blobStorageDir = storageRoot
                .resolve("files")
                .resolve(firstTwoChars);

        Files.createDirectories(blobStorageDir); // Ensure directory exists

        Path targetPath = blobStorageDir.resolve(sha256);

        // 5. Save the file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 6. Create or update StoredFile status
        StoredFile storedFile = existingStoredFileOpt.orElseGet(() -> StoredFile.builder()
                .sha256(sha256)
                .size(file.getSize())
                .status(StoredFileStatus.Pending) // Default to PENDING, will be updated to UPLOADED
                .createdAt(Instant.now())
                .build());
        storedFile.setStatus(StoredFileStatus.Uploaded);
        storedFile.setSize(file.getSize()); // Ensure size is correct
        storedFileRepo.save(storedFile);

        return sha256;
    }

    @Transactional
    public void updateVersionStatus(UUID modpackId, UUID versionId, VersionStatus newStatus, String ownerToken) {
        Modpack modpack = findModpackById(modpackId);

        ModpackVersion version = findVersionById(versionId);

        // Authorize owner
        if (!modpack.getOwner().getToken().equals(ownerToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this modpack.");
        }

        version.setStatus(newStatus);
        versionRepo.save(version);
    }

    public ModpackVersion getVersionById(UUID versionId) {
        return findVersionById(versionId);
    }

    public List<ModpackVersionDTO> getModpackVersions(String modpackId, String providedCode, String ownerToken) {
        Modpack modpack = getModpackIfAccessible(UUID.fromString(modpackId), providedCode);

        boolean isOwner = ownerToken != null && ownerToken.equals(modpack.getOwner().getToken());

        return modpack.getVersions().stream()
                .filter(version -> isOwner || version.getStatus() != VersionStatus.Draft)
                .map(ModpackVersionDTO::from)
                .sorted(Comparator.comparing(ModpackVersionDTO::getCreatedAt).reversed())
                .toList();
    }

    public Path getModpackManifestFile(UUID versionId, String providedCode) throws IOException {
        ModpackVersion version = findVersionById(versionId);
        getModpackIfAccessible(version.getModpack().getId(), providedCode); // Check access to modpack

        if (version.getStatus() == VersionStatus.Draft) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot retrieve manifest for a DRAFT version.");
        }

        // Find the manifest.json file within this version
        ModpackVersionFile manifestFile = version.getFiles().stream()
                .filter(mvf -> "manifest.json".equals(mvf.getFilePath()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "manifest.json not found for version " + versionId));

        StoredFile storedManifest = manifestFile.getStoredFile();

        if (storedManifest.getStatus() != StoredFileStatus.Uploaded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manifest file for version " + versionId + " is not yet uploaded.");
        }

        Path manifestPath = getStoredFilePath(storedManifest.getSha256());

        if (!Files.exists(manifestPath)) {
            throw new IOException("Manifest file not found on disk: " + storedManifest.getSha256());
        }

        return manifestPath;
    }

    public Path getFullVersionZip(String modpackId, UUID versionId) throws IOException {
        Modpack modpack = findModpackById(modpackId);

        ModpackVersion version = findVersionById(versionId);

        // Ensure the version is not in DRAFT status for full download
        if (version.getStatus() == VersionStatus.Draft) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot download a DRAFT version.");
        }

        // Create a temporary zip file
        Path tempZipFile = Files.createTempFile(modpackId + "-" + version.getSemver(), ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            for (ModpackVersionFile fileEntry : version.getFiles()) {
                StoredFile storedFile = fileEntry.getStoredFile();
                if (storedFile.getStatus() != StoredFileStatus.Uploaded) {
                    throw new IOException("File " + fileEntry.getFilePath() + " (SHA256: " + storedFile.getSha256() + ") is not uploaded yet.");
                }

                Path sourceFile = getStoredFilePath(storedFile.getSha256());
                if (Files.exists(sourceFile)) {
                    ZipEntry zipEntry = new ZipEntry(fileEntry.getFilePath());
                    zos.putNextEntry(zipEntry);
                    Files.copy(sourceFile, zos);
                    zos.closeEntry();
                } else {
                    System.err.println("File not found on disk for SHA256: " + storedFile.getSha256());
                    throw new IOException("Stored file not found on disk: " + storedFile.getSha256());
                }
            }
        }
        return tempZipFile;
    }

    public ModpackTreeDTO getModpackVersionTree(UUID versionId) {
        ModpackVersion version = findVersionById(versionId);

        List<ModpackFileDTO> fileDTOs = version.getFiles().stream()
                .map(mvf -> ModpackFileDTO.builder()
                        .filePath(mvf.getFilePath())
                        .sha256(mvf.getStoredFile().getSha256())
                        .fileSize(mvf.getStoredFile().getSize())
                        .build())
                .toList();

        ModpackTreeDTO treeDTO = new ModpackTreeDTO();
        treeDTO.setFiles(fileDTOs);
        return treeDTO;
    }

    public Path downloadStoredFile(String sha256) throws IOException {
        StoredFile storedFile = storedFileRepo.findById(sha256)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored file with SHA256 " + sha256 + " not found."));

        if (storedFile.getStatus() != StoredFileStatus.Uploaded) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File with SHA256 " + sha256 + " is not yet uploaded.");
        }

        Path filePath = getStoredFilePath(sha256);
        if (!Files.exists(filePath)) {
            throw new IOException("Stored file not found on disk: " + sha256);
        }
        return filePath;
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


    public boolean authorizeModpackAction(UUID modpackId, String token, String password) {
        return modpackRepo.findById(modpackId)
                .map(m -> m.getOwner().getToken().equals(token) &&
                        m.getModpackPassword().equals(password))
                .orElse(false);
    }

    public Modpack getModpackIfAccessible(UUID id, String providedCode) {
        Modpack modpack = findModpackById(id);

        // Dacă modpack-ul este privat, verificăm codul
        if (!modpack.isPublic()) {
            if (providedCode == null || !providedCode.equals(modpack.getSharingCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This modpack is private. Valid sharing code required.");
            }
        }

        return modpack;
    }

}