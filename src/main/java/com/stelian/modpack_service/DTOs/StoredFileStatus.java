package com.stelian.modpack_service.DTOs;

public enum StoredFileStatus {
    Pending,    // Metadata exists, but file content has not been uploaded yet
    Uploaded    // File content has been successfully uploaded
}