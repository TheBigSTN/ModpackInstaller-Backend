package com.stelian.modpack_service.DTOs;

import com.stelian.modpack_service.Entities.Modpack;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ModpackDTO {

    private UUID id;
    private String name;
    private UserDTO owner;
    private String gameVersion;
    private ModLoaderType loader;
    private String loaderVersion;
    private ModpackVersionDTO latestVersion;

    @Nullable
    private String password;

    @Nullable
    private String shareCode;

    public static ModpackDTO from(Modpack modpack) {
        return baseBuilder(modpack)
                .build();
    }

    public static ModpackDTO fromAuthorized(Modpack modpack) {
        return baseBuilder(modpack)
                .password(modpack.getModpackPassword())
                .build();
    }

    private static ModpackDTO.ModpackDTOBuilder baseBuilder(Modpack modpack) {
        var builder = ModpackDTO.builder()
                .id(modpack.getId())
                .name(modpack.getName())
                .owner(UserDTO.from(modpack.getOwner()))
                .gameVersion(modpack.getGameVersion())
                .loader(modpack.getLoader())
                .shareCode(modpack.getSharingCode())
                .loaderVersion(modpack.getLoaderVersion());

        if (modpack.getLatestVersion() != null) {
            builder.latestVersion(ModpackVersionDTO.from(modpack.getLatestVersion()));
        }

        return builder;
    }
}