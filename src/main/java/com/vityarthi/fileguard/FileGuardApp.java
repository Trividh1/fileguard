package com.vityarthi.fileguard;

import com.vityarthi.fileguard.core.BaselineEngine;
import com.vityarthi.fileguard.core.LiveMonitor;
import com.vityarthi.fileguard.core.VerificationEngine;
import com.vityarthi.fileguard.exception.FileGuardException;
import com.vityarthi.fileguard.model.FileRecord;
import com.vityarthi.fileguard.model.IntegrityReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FileGuardApp {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String command = args[0].toLowerCase();
        String targetDirPath = args[1];
        Path targetDir = Paths.get(targetDirPath).toAbsolutePath().normalize();

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            System.err.println("Error: Provided path is not a valid directory: " + targetDir);
            System.exit(1);
        }

        BaselineEngine baselineEngine = new BaselineEngine();
        VerificationEngine verificationEngine = new VerificationEngine();

        try {
            switch (command) {
                case "init":
                    System.out.println("Initializing integrity baseline for: " + targetDir);
                    Map<String, FileRecord> baseline = baselineEngine.buildAndSaveBaseline(targetDir);
                    System.out.println("Successfully indexed " + baseline.size() + " files.");
                    System.out.println("Baseline manifest stored at: " + targetDir.resolve(BaselineEngine.MANIFEST_FILE_NAME));
                    break;

                case "check":
                    System.out.println("Auditing integrity for: " + targetDir);
                    IntegrityReport report = verificationEngine.verify(targetDir);
                    report.printConsoleSummary();
                    if (!report.isClean()) {
                        System.exit(2);
                    }
                    break;

                case "watch":
                    System.out.println("Loading baseline for live monitoring...");
                    Map<String, FileRecord> watchBaseline = baselineEngine.loadBaseline(targetDir);
                    LiveMonitor monitor = new LiveMonitor(targetDir, watchBaseline);
                    Thread monitorThread = new Thread(monitor, "FileGuard-LiveMonitor");
                    monitorThread.start();
                    try {
                        monitorThread.join();
                    } catch (InterruptedException e) {
                        monitor.stop();
                        System.out.println("Monitoring stopped.");
                    }
                    break;

                case "status":
                    Map<String, FileRecord> statusBaseline = baselineEngine.loadBaseline(targetDir);
                    System.out.println("Baseline status for: " + targetDir);
                    System.out.println("Total tracked files: " + statusBaseline.size());
                    for (FileRecord r : statusBaseline.values()) {
                        System.out.println(" - " + r);
                    }
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (FileGuardException e) {
            System.err.println("\n[ERROR] " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("==================================================");
        System.out.println("    FileGuard: Cryptographic Integrity Auditor    ");
        System.out.println("==================================================");
        System.out.println("Usage: java -jar fileguard.jar <command> <directory_path>\n");
        System.out.println("Commands:");
        System.out.println("  init   <path>   Generate a SHA-256 cryptographic baseline manifest");
        System.out.println("  check  <path>   Verify current directory state against baseline");
        System.out.println("  watch  <path>   Continuously monitor directory for live tampering");
        System.out.println("  status <path>   View summary of tracked baseline records");
        System.out.println("==================================================");
    }
}
