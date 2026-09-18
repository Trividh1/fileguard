package com.vityarthi.fileguard.core;

import com.vityarthi.fileguard.exception.FileGuardException;
import com.vityarthi.fileguard.model.FileRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class BaselineEngine {
    public static final String MANIFEST_FILE_NAME = ".fileguard_baseline.manifest";

    public Map<String, FileRecord> buildAndSaveBaseline(Path rootDirectory) throws FileGuardException {
        if (!Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory)) {
            throw new FileGuardException("Target path is not an existing directory: " + rootDirectory);
        }

        Map<String, FileRecord> manifest = new HashMap<>();
        Path manifestPath = rootDirectory.resolve(MANIFEST_FILE_NAME);

        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.getFileName().toString().equals(MANIFEST_FILE_NAME))
                  .forEach(filePath -> {
                      try {
                          String relative = rootDirectory.relativize(filePath).toString();
                          String hash = Hasher.computeSHA256(filePath);
                          long size = Files.size(filePath);
                          long lastModified = Files.getLastModifiedTime(filePath).toMillis();

                          FileRecord record = new FileRecord(relative, hash, size, lastModified);
                          manifest.put(relative, record);
                      } catch (Exception ex) {
                          System.err.println("Warning: Could not index file " + filePath + ": " + ex.getMessage());
                      }
                  });
        } catch (IOException e) {
            throw new FileGuardException("Failed to walk directory structure: " + e.getMessage(), e);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(manifestPath.toFile()))) {
            for (FileRecord record : manifest.values()) {
                writer.write(record.toCsvLine());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new FileGuardException("Failed to persist baseline manifest: " + e.getMessage(), e);
        }

        return manifest;
    }

    public Map<String, FileRecord> loadBaseline(Path rootDirectory) throws FileGuardException {
        Path manifestPath = rootDirectory.resolve(MANIFEST_FILE_NAME);
        if (!Files.exists(manifestPath)) {
            throw new FileGuardException("Baseline manifest not found in: " + rootDirectory + 
                    "\nRun 'fileguard init <dir>' first.");
        }

        Map<String, FileRecord> baseline = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(manifestPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    FileRecord record = FileRecord.fromCsvLine(line);
                    baseline.put(record.getRelativePath(), record);
                }
            }
        } catch (IOException e) {
            throw new FileGuardException("Error reading baseline manifest: " + e.getMessage(), e);
        }

        return baseline;
    }
}
