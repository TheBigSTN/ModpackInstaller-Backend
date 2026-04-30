package com.stelian.modpack_service.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class InstalledModInfo {

    @JsonProperty("ProjectId")
    private String projectId = "";

    @JsonProperty("VersionId")
    private String versionId = "";

    @JsonProperty("Source")
    private ModSource source = ModSource.Local;

    @JsonProperty("Title")
    private String title = "";

    @JsonProperty("Filename")
    private String filename = "";

    @JsonProperty("DownloadUrl")
    private String downloadUrl = "";

    @JsonProperty("IconUrl")
    private String iconUrl = "";

    @JsonProperty("Enabled")
    private boolean enabled = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstalledModInfo that)) return false;
        return Objects.equals(projectId, that.projectId) &&
                Objects.equals(versionId, that.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, versionId);
    }
}