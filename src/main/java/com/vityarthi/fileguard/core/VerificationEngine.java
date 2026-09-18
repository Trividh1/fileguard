package com.vityarthi.fileguard.core;

import com.vityarthi.fileguard.exception.FileGuardException;
import com.vityarthi.fileguard.model.FileRecord;
import com.vityarthi.fileguard.model.IntegrityReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class VerificationEngine {
    private final BaselineEngine baselineEngine;

    public VerificationEngine() {
        this.baselineEngine = new BaselineEngine();
    }

    public IntegrityReport verify(Path rootDirectory) throws FileGuardException {
        Map<String, FileRecord> baseline = baselineEngine.loadBaseline(rootDirectory);
        IntegrityReport report = new IntegrityReport();

        Map<String, Path> currentFiles = new HashMap<>();
        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.getFileName().toString().equals(BaselineEngine.MANIFEST_FILE_NAME))
                  .forEach(p -> currentFiles.put(rootDirectory.relativize(p).toString(), p));
        } catch (IOException e) {
            throw new FileGuardException("Failed to scan current directory state: " + e.getMessage(), e);
        }

        for (Map.Entry<String, FileRecord> entry : baseline.entrySet()) {
            String relPath = entry.getKey();
            FileRecord baseRecord = entry.getValue();

            if (!currentFiles.containsKey(relPath)) {
                report.addDeleted(relPath);
            } else {
                Path currentPath = currentFiles.get(relPath);
                String currentHash = Hasher.computeSHA256(currentPath);

                if (currentHash.equalsIgnoreCase(baseRecord.getSha256Hash())) {
                    report.addIntact(relPath);
                } else {
                    report.addModified(relPath);
                }
            }
        }

        for (String currentRelPath : currentFiles.keySet()) {
            if (!baseline.containsKey(currentRelPath)) {
                report.addUntracked(currentRelPath);
            }
        }

        return report;
    }
}
