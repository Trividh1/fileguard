package com.vityarthi.fileguard;

import com.vityarthi.fileguard.core.BaselineEngine;
import com.vityarthi.fileguard.core.Hasher;
import com.vityarthi.fileguard.core.VerificationEngine;
import com.vityarthi.fileguard.exception.FileGuardException;
import com.vityarthi.fileguard.model.FileRecord;
import com.vityarthi.fileguard.model.IntegrityReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FileGuardTest {

    @TempDir
    Path tempDir;

    private BaselineEngine baselineEngine;
    private VerificationEngine verificationEngine;

    @BeforeEach
    void setUp() {
        baselineEngine = new BaselineEngine();
        verificationEngine = new VerificationEngine();
    }

    @Test
    void testHashingDeterminism() throws IOException, FileGuardException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Hello FileGuard Cryptographic Test");

        String hash1 = Hasher.computeSHA256(testFile);
        String hash2 = Hasher.computeSHA256(testFile);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex string should be 64 characters");
        assertEquals(hash1, hash2, "Hashing identical content must yield identical digests");
    }

    @Test
    void testBaselineCreationAndCleanVerification() throws IOException, FileGuardException {
        Files.writeString(tempDir.resolve("fileA.txt"), "Content A");
        Files.writeString(tempDir.resolve("fileB.txt"), "Content B");

        Map<String, FileRecord> baseline = baselineEngine.buildAndSaveBaseline(tempDir);
        assertEquals(2, baseline.size());

        IntegrityReport report = verificationEngine.verify(tempDir);
        assertTrue(report.isClean(), "Newly created baseline should verify clean");
        assertEquals(2, report.getIntactFiles().size());
        assertEquals(0, report.getModifiedFiles().size());
    }

    @Test
    void testModificationDetection() throws IOException, FileGuardException {
        Path file = tempDir.resolve("data.log");
        Files.writeString(file, "Original Unmodified State");

        baselineEngine.buildAndSaveBaseline(tempDir);

        Files.writeString(file, "Unauthorized Tampered Modification!");

        IntegrityReport report = verificationEngine.verify(tempDir);
        assertFalse(report.isClean(), "Tampered file should fail verification");
        assertEquals(1, report.getModifiedFiles().size());
        assertEquals("data.log", report.getModifiedFiles().get(0));
    }

    @Test
    void testDeletionDetection() throws IOException, FileGuardException {
        Path file1 = tempDir.resolve("doc1.txt");
        Path file2 = tempDir.resolve("doc2.txt");
        Files.writeString(file1, "Doc 1");
        Files.writeString(file2, "Doc 2");

        baselineEngine.buildAndSaveBaseline(tempDir);

        Files.delete(file2);

        IntegrityReport report = verificationEngine.verify(tempDir);
        assertFalse(report.isClean());
        assertEquals(1, report.getDeletedFiles().size());
        assertEquals("doc2.txt", report.getDeletedFiles().get(0));
    }
}
