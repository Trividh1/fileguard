package com.vityarthi.fileguard.core;

import com.vityarthi.fileguard.model.FileRecord;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LiveMonitor implements Runnable {
    private final Path rootDirectory;
    private final Map<String, FileRecord> baseline;
    private volatile boolean running = true;

    public LiveMonitor(Path rootDirectory, Map<String, FileRecord> baseline) {
        this.rootDirectory = rootDirectory;
        this.baseline = baseline;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("[MONITOR] Starting real-time watcher on: " + rootDirectory);
        System.out.println("[MONITOR] Press Ctrl+C or kill process to terminate monitoring.\n");

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            rootDirectory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );

            while (running) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (filename.toString().equals(BaselineEngine.MANIFEST_FILE_NAME)) {
                        continue;
                    }

                    String timestamp = LocalTime.now().format(timeFmt);
                    Path fullPath = rootDirectory.resolve(filename);
                    String relPath = filename.toString();

                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        try {
                            String newHash = Hasher.computeSHA256(fullPath);
                            FileRecord original = baseline.get(relPath);
                            if (original != null && !newHash.equalsIgnoreCase(original.getSha256Hash())) {
                                System.out.printf("[%s] [ALERT - MODIFIED] File: %s | Tampered Checksum: %s%n", 
                                        timestamp, relPath, newHash.substring(0, 12));
                            }
                        } catch (Exception ignored) {
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        System.out.printf("[%s] [WARNING - CREATED] New file introduced: %s%n", timestamp, relPath);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        System.out.printf("[%s] [ALERT - DELETED] Critical file removed: %s%n", timestamp, relPath);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("[MONITOR ERROR] WatchService failure: " + e.getMessage());
        }
    }
}
