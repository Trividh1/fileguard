package com.vityarthi.fileguard.model;

import java.io.Serializable;

/**
 * Encapsulates the metadata and cryptographic checksum of a tracked file.
 */
public class FileRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String relativePath;
    private final String sha256Hash;
    private final long fileSizeBytes;
    private final long lastModifiedEpochMs;

    public FileRecord(String relativePath, String sha256Hash, long fileSizeBytes, long lastModifiedEpochMs) {
        this.relativePath = relativePath;
        this.sha256Hash = sha256Hash;
        this.fileSizeBytes = fileSizeBytes;
        this.lastModifiedEpochMs = lastModifiedEpochMs;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getLastModifiedEpochMs() {
        return lastModifiedEpochMs;
    }

    public String toCsvLine() {
        return String.format("%s,%s,%d,%d", relativePath, sha256Hash, fileSizeBytes, lastModifiedEpochMs);
    }

    public static FileRecord fromCsvLine(String line) {
        String[] parts = line.split(",", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Corrupted manifest entry: " + line);
        }
        return new FileRecord(parts[0], parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
    }

    @Override
    public String toString() {
        return String.format("[%s] (SHA-256: %s... | %d bytes)", relativePath, 
                sha256Hash.substring(0, Math.min(10, sha256Hash.length())), fileSizeBytes);
    }
}
