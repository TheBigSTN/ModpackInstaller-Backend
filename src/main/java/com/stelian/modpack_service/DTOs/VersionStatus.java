package com.stelian.modpack_service.DTOs;

public enum VersionStatus {
    Draft,      // Initial state, only owner can see and upload files
    Private,    // All files uploaded, owner can set to TESTING or RELEASE
    Testing,    // Available for specific testers
    Release     // Publicly available
}