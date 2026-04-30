package com.stelian.modpack_service.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stelian.modpack_service.DTOs.*;
import com.stelian.modpack_service.Entities.Modpack;
import com.stelian.modpack_service.Entities.ModpackOwner;
import com.stelian.modpack_service.Entities.ModpackVersion;
import com.stelian.modpack_service.Objects.InstalledModInfo;
import com.stelian.modpack_service.Objects.ModpackManifest;
import com.stelian.modpack_service.Objects.PatchManifest;
import com.stelian.modpack_service.Repositories.ModpackRepository;
import com.stelian.modpack_service.Repositories.ModpackVersionRepository;
import com.stelian.modpack_service.Repositories.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ModpackService {

    private final ModpackRepository modpackRepo;
    private final ModpackVersionRepository versionRepo;
    private final OwnerRepository ownerRepo;

    private Path storageRoot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.storage.root-path}")
    private void setStorageRoot(String path) {
        this.storageRoot = Paths.get(path);
    }


    @Transactional
    public ModpackOwner registerNewOwner(String nickname) {
        String token = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ModpackOwner owner = ModpackOwner.builder()
                .ownerToken(token)
                .nickname(nickname)
                .createdAt(LocalDateTime.now())
                .build();
        return ownerRepo.save(owner);
    }

    public List<Modpack> getModpacksByOwner(String ownerToken) {
        return modpackRepo.findByOwnerOwnerToken(ownerToken);
    }

    public List<PublicModpackResponseDTO> findAllPublic() {
        return modpackRepo.findByIsPublicTrue().stream()
                .map(m -> PublicModpackResponseDTO.builder()
                        .id(m.getId())
                        .modpackName(m.getName())
                        .authorName(m.getName())
                        .gameVersion(m.getGameVersion())
                        .loader(m.getLoader())
                        .loaderVersion(m.getLoaderVersion())
                        .latestVersion(m.getLatestVersion())
                        .createdAt(m.getCreatedAt())
                        .modifiedAt(m.getUpdatedAt())
                        .build())
                .toList();
    }


//    @Transactional
//    public void processUpload(ModpackMetadataDTO dto, MultipartFile zipFile) throws IOException {
//
//        /* =========================================================
//           1. CREATE / UPDATE MODPACK (MANAGED ENTITY)
//           ========================================================= */
//
//        Modpack modpack = modpackRepo.findById(dto.getId())
//                .orElseGet(() -> {
//                    Modpack m = new Modpack();
//                    m.setId(dto.getId());
//                    m.setCreatedAt(LocalDateTime.now());
//                    return m;
//                });
//
//        modpack.setName(dto.getName());
//        modpack.setOwnerNickname(dto.getOwnerNickname());
//        modpack.setSharingCode(dto.getSharingCode());
//        modpack.setPublic(dto.isPublic());
//        modpack.setGameVersion(dto.getGameVersion());
//        modpack.setLoader(dto.getLoader() != null ? dto.getLoader().toString() : null);
//        modpack.setLoaderVersion(dto.getLoaderVersion());
//        modpack.setAuthor(dto.getAuthor());
//        modpack.setDescription(dto.getDescription());
//        modpack.setInstallPath(dto.getInstallPath());
//        modpack.setUpdatedAt(LocalDateTime.now());
//
//        modpack = modpackRepo.saveAndFlush(modpack); // 🔥 IMPORTANT
//
//
//        /* =========================================================
//           2. DIRECTORY STRUCTURE
//           ========================================================= */
//
//        Path modpackDir = storageRoot.resolve(modpack.getId());
//        Path versionsDir = modpackDir.resolve("versions");
//        Path patchesDir = modpackDir.resolve("patches");
//
//        Files.createDirectories(versionsDir);
//        Files.createDirectories(patchesDir);
//
//
//        /* =========================================================
//           3. SAVE FULL ZIP
//           ========================================================= */
//
//        Path newZipPath = versionsDir.resolve("v" + dto.getVersion() + ".zip");
//        Files.copy(zipFile.getInputStream(), newZipPath, StandardCopyOption.REPLACE_EXISTING);
//
//
//        /* =========================================================
//           4. GENERATE PATCH (IF NEEDED)
//           ========================================================= */
//
//        String patchPath = null;
//
//        if (dto.getVersion() > 1) {
//            versionRepo.findByModpackAndVersionNumber(modpack, dto.getVersion() - 1)
//                    .ifPresent(prev -> {
//                        try {
//                            Path oldZip = Paths.get(prev.getZipPath());
//                            Path patchOut = patchesDir.resolve(
//                                    "v" + (dto.getVersion() - 1) + "_to_v" + dto.getVersion() + ".zip"
//                            );
//                            generatePatch(oldZip, newZipPath, patchOut);
//                        } catch (IOException e) {
//                            throw new UncheckedIOException(e);
//                        }
//                    });
//
//            patchPath = patchesDir
//                    .resolve("v" + (dto.getVersion() - 1) + "_to_v" + dto.getVersion() + ".zip")
//                    .toString();
//        }
//
//
//        /* =========================================================
//           5. SAVE VERSION
//           ========================================================= */
//
//        ModpackVersion version = ModpackVersion.builder()
//                .modpack(modpack) // ✅ MANAGED ENTITY
//                .versionNumber(dto.getVersion())
//                .zipPath(newZipPath.toString())
//                .patchPath(patchPath)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        versionRepo.save(version);
//
//
//        /* =========================================================
//           6. UPDATE LATEST VERSION
//           ========================================================= */
//
//        modpack.setLatestVersion(dto.getVersion());
//        modpackRepo.save(modpack);
//    }

    @Transactional
    public ModpackResponseDTO createInitialModpack(ModpackCreateRequestDTO dto, String ownerToken) {
        // 1. Validare Owner (Folosim nickname-ul din contul real pentru Author/OwnerNickname)
        ModpackOwner owner = ownerRepo.findById(ownerToken)
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

        // 🔥 GUARDRAIL: Preluăm Author și OwnerNickname DOAR din profilul Owner-ului
        modpack.setAuthor(owner.getNickname());
        modpack.setOwnerNickname(owner.getNickname());

        modpack.setCreatedAt(Instant.now());
        modpack.setUpdatedAt(Instant.now());

        // NOTA: installPath și latestVersion rămân default/null până la primul upload de fișier

        Modpack saved = modpackRepo.save(modpack);
        return mapToResponseDTO(saved);
    }

    private ModpackResponseDTO mapToResponseDTO(Modpack m) {
        return ModpackResponseDTO.builder()
                .id(m.getId())
                .name(m.getName())
                .ownerNickname(m.getOwnerNickname())
                .modpackPassword(m.getModpackPassword()) // Trimitem parola DOAR la creare/update
                .ownerToken(m.getOwner().getOwnerToken()) // Trimitem doar ID-ul owner-ului, nu tot obiectul
                .author(m.getAuthor())
                .gameVersion(m.getGameVersion())
                .loader(m.getLoader().toString())
                .build();
    }

    public boolean validateOwner(String token) {
        return !ownerRepo.existsById(token);
    }

    @Transactional
    public void deleteModpack(String id) {
        Modpack modpack = modpackRepo.findById(id).orElse(null);
        if (modpack == null) return;

        // 1. Ștergem fișierele fizice
        Path modpackDir = storageRoot.resolve(id);
        try {
            if (Files.exists(modpackDir)) {
                // Folosim un utilitar sau stream pentru a șterge recursiv folderul
                Files.walk(modpackDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu am putut șterge fișierele de pe disc.");
        }

        // 2. Ștergem din DB (cascade-ul se va ocupa de versiuni dacă e setat)
        modpackRepo.delete(modpack);
    }
    @Transactional
    public ModpackResponseDTO updateMetadata(String id, ModpackRequestDTO dto) {
        Modpack modpack = modpackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modpack not found"));

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
        // NU preluăm valorile din DTO. Păstrăm ce avem deja în DB (care au fost setate la creare din Account)
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
        return mapToResponseDTO(saved); // Returnăm DTO-ul curat, nu entitatea
    }
    @Transactional
    public void addVersion(String modpackId, int versionNumber, MultipartFile zipFile) throws IOException {
        Modpack modpack = modpackRepo.findById(modpackId)
                .orElseThrow(() -> new RuntimeException("Modpack not found"));

        // Verificăm dacă versiunea există deja pentru a evita duplicatele
        if (versionRepo.findByModpackAndVersionNumber(modpack, versionNumber).isPresent()) {
            throw new RuntimeException("Version " + versionNumber + " already exists!");
        } else if (modpack.getLatestVersion() > versionNumber) {
            throw new RuntimeException("There is a newer version already on the server");
        }

        // Logică stocare fișier (identică ce aveai la punctul 3 din codul tău)
        Path modpackDir = storageRoot.resolve(modpack.getId());
        Path versionsDir = modpackDir.resolve("versions");
        Files.createDirectories(versionsDir);

        Path newZipPath = versionsDir.resolve("v" + versionNumber + ".zip");
        Files.copy(zipFile.getInputStream(), newZipPath, StandardCopyOption.REPLACE_EXISTING);

        // Generare patch (identică logica ta anterioară)
        String patchPath = generatePatchIfPossible(modpack, versionNumber, newZipPath);

        // Salvare versiune
        ModpackVersion version = ModpackVersion.builder()
                .modpack(modpack)
                .versionNumber(versionNumber)
                .zipPath(newZipPath.toString())
                .patchPath(patchPath)
                .build();

        versionRepo.save(version);

        // Update latest version in parent
        modpack.setLatestVersion(versionNumber);
        modpackRepo.save(modpack);
    }

    private void generatePatch(Path oldZip, Path newZip, Path patchOut) throws IOException {

        Map<String, byte[]> oldFiles = readZip(oldZip);
        Map<String, byte[]> newFiles = readZip(newZip);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(patchOut))) {

            // 🔹 New or modified files (exclude manifest.json)
            for (var entry : newFiles.entrySet()) {

                String name = entry.getKey();

                if (name.equals("manifest.json"))
                    continue;

                byte[] old = oldFiles.get(name);

                if (old == null || !Arrays.equals(old, entry.getValue())) {
                    zos.putNextEntry(new ZipEntry("files/" + name));
                    zos.write(entry.getValue());
                    zos.closeEntry();
                }
            }

            // 🔹 Deleted files (exclude manifest.json)
            List<String> deleted = oldFiles.keySet().stream()
                    .filter(f -> !newFiles.containsKey(f))
                    .filter(f -> !f.equals("manifest.json"))
                    .toList();

            if (!deleted.isEmpty()) {
                zos.putNextEntry(new ZipEntry("delete.txt"));
                zos.write(String.join("\n", deleted).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // 🔹 Semantic InstalledMods diff
            PatchManifest patchManifest = generatePatchManifest(oldZip, newZip);

//            if (!patchManifest.isEmpty()) {
                zos.putNextEntry(new ZipEntry("patch-manifest.json"));

                String json = objectMapper.writeValueAsString(patchManifest);
                zos.write(json.getBytes(StandardCharsets.UTF_8));

                zos.closeEntry();
//            }
        }
    }

    private PatchManifest generatePatchManifest(Path oldZip, Path newZip) throws IOException {

        ModpackManifest oldManifest = readManifest(oldZip);
        ModpackManifest newManifest = readManifest(newZip);

        Set<InstalledModInfo> oldMods =
                new HashSet<>(oldManifest.getInstalledMods());

        Set<InstalledModInfo> newMods =
                new HashSet<>(newManifest.getInstalledMods());

        PatchManifest patch = new PatchManifest();

        // Added
        for (InstalledModInfo mod : newMods) {
            if (!oldMods.contains(mod)) {
                patch.getAdded().add(mod);
            }
        }

        // Removed
        for (InstalledModInfo mod : oldMods) {
            if (!newMods.contains(mod)) {
                patch.getRemoved().add(mod);
            }
        }

        return patch;
    }

    private ModpackManifest readManifest(Path zipPath) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals("manifest.json")) {
                    byte[] data = zis.readAllBytes();
                    return objectMapper.readValue(data, ModpackManifest.class);
                }
            }
        }

        throw new IOException("manifest.json not found in " + zipPath);
    }

    private Map<String, byte[]> readZip(Path zipPath) throws IOException {
        Map<String, byte[]> files = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    files.put(entry.getName(), zis.readAllBytes());
                }
            }
        }
        return files;
    }

    public Path getFullVersionZip(String modpackId, int version) {
        return storageRoot
                .resolve(modpackId)
                .resolve("versions")
                .resolve("v" + version + ".zip");
    }

    private String generatePatchIfPossible(Modpack modpack, int currentVersion, Path newZipPath) {
        // Dacă e prima versiune, nu avem ce patch să generăm
        if (currentVersion <= 1) {
            return null;
        }

        // Căutăm versiunea imediat anterioară
        Optional<ModpackVersion> prevVersionOpt = versionRepo.findByModpackAndVersionNumber(modpack, currentVersion - 1);

        if (prevVersionOpt.isPresent()) {
            try {
                ModpackVersion prev = prevVersionOpt.get();
                Path oldZip = Paths.get(prev.getZipPath());

                // Definim unde va fi salvat patch-ul
                Path patchesDir = storageRoot.resolve(modpack.getId()).resolve("patches");
                Files.createDirectories(patchesDir);

                Path patchOut = patchesDir.resolve("v" + (currentVersion - 1) + "_to_v" + currentVersion + ".zip");

                // Apelăm metoda de comparare ZIP-uri pe care o ai deja definită
                generatePatch(oldZip, newZipPath, patchOut);

                return patchOut.toString();
            } catch (IOException e) {
                // Logăm eroarea dar poate nu vrem să oprim tot upload-ul dacă patch-ul eșuează
                System.err.println("Could not generate patch: " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    public Path getCombinedPatch(String modpackId, int from, int to) throws IOException {
        if (from >= to) throw new IllegalArgumentException("from must be < to");

        Path modDir = storageRoot.resolve(modpackId);
        Path patchesDir = modDir.resolve("patches");
        Files.createDirectories(patchesDir);

        Path combinedPatch = patchesDir.resolve("v" + from + "_to_v" + to + ".zip");

        Map<String, byte[]> combinedFiles = new HashMap<>();
        Set<String> combinedDeleted = new HashSet<>();

        // folosim Map pentru manifest ca să evităm duplicate
        Map<String, InstalledModInfo> addedMap = new HashMap<>();
        Map<String, InstalledModInfo> removedMap = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();

        for (int v = from; v < to; v++) {

            Path patchZip = patchesDir.resolve("v" + v + "_to_v" + (v + 1) + ".zip");
            if (!Files.exists(patchZip)) {
                throw new FileNotFoundException("Patch for " + v + " -> " + (v + 1) + " not found");
            }

            Map<String, byte[]> patchFiles = readZip(patchZip);

            // ======================
            // 1️⃣ Aplicăm delete.txt
            // ======================
            byte[] deleteData = patchFiles.remove("delete.txt");
            if (deleteData != null) {
                String[] deleted = new String(deleteData, StandardCharsets.UTF_8).split("\n");

                for (String path : deleted) {
                    path = path.trim();
                    if (path.isEmpty()) continue;

                    combinedFiles.remove(path);
                    combinedDeleted.add(path);
                }
            }

            // ======================
            // 2️⃣ Aplicăm fișierele noi
            // ======================
            for (var entry : patchFiles.entrySet()) {
                String name = entry.getKey();

                if (name.equals("patch-manifest.json") || name.equals("delete.txt"))
                    continue;

                combinedFiles.put(name, entry.getValue());

                // dacă a fost șters anterior dar reapare
                combinedDeleted.remove(name);
            }

            // ======================
            // 3️⃣ Aplicăm manifest-ul
            // ======================
            byte[] manifestData = patchFiles.get("patch-manifest.json");
            if (manifestData != null) {

                PatchManifest pm = mapper.readValue(manifestData, PatchManifest.class);

                // ---- ADDED ----
                for (InstalledModInfo mod : pm.getAdded()) {

                    String key = mod.getProjectId();

                    // dacă fusese removed anterior → anulăm removed
                    removedMap.remove(key);

                    // suprascriem / adăugăm
                    addedMap.put(key, mod);
                }

                // ---- REMOVED ----
                for (InstalledModInfo mod : pm.getRemoved()) {

                    String key = mod.getProjectId();

                    // dacă fusese added anterior → anulăm added
                    addedMap.remove(key);

                    removedMap.put(key, mod);
                }
            }
        }

        // ======================
        // Construim manifest final
        // ======================
        PatchManifest finalManifest = new PatchManifest();
        finalManifest.setAdded(new ArrayList<>(addedMap.values()));
        finalManifest.setRemoved(new ArrayList<>(removedMap.values()));
        finalManifest.setEmpty(
                finalManifest.getAdded().isEmpty() &&
                        finalManifest.getRemoved().isEmpty()
        );

        // ======================
        // Scriem patch-ul final
        // ======================
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(combinedPatch))) {

            for (var e : combinedFiles.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue());
                zos.closeEntry();
            }

            if (!combinedDeleted.isEmpty()) {
                zos.putNextEntry(new ZipEntry("delete.txt"));
                zos.write(String.join("\n", combinedDeleted).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.putNextEntry(new ZipEntry("patch-manifest.json"));
            zos.write(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(finalManifest));
            zos.closeEntry();
        }

        return combinedPatch;
    }


    public boolean authorizeModpackAction(String modpackId, String token, String password) {
        return modpackRepo.findById(modpackId)
                .map(m -> m.getOwner().getOwnerToken().equals(token) &&
                        m.getModpackPassword().equals(password))
                .orElse(false);
    }

    public Modpack getModpackIfAccessible(String id, String providedCode) {
        Modpack modpack = modpackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modpack not found"));

        // Dacă modpack-ul este privat, verificăm codul
        if (!modpack.isPublic()) {
            if (providedCode == null || !providedCode.equals(modpack.getSharingCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This modpack is private. Valid sharing code required.");
            }
        }

        return modpack;
    }

}
