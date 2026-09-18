package com.vityarthi.fileguard.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntegrityReport {
    private final LocalDateTime timestamp;
    private final List<String> intactFiles = new ArrayList<>();
    private final List<String> modifiedFiles = new ArrayList<>();
    private final List<String> deletedFiles = new ArrayList<>();
    private final List<String> untrackedFiles = new ArrayList<>();

    public IntegrityReport() {
        this.timestamp = LocalDateTime.now();
    }

    public void addIntact(String path) { intactFiles.add(path); }
    public void addModified(String path) { modifiedFiles.add(path); }
    public void addDeleted(String path) { deletedFiles.add(path); }
    public void addUntracked(String path) { untrackedFiles.add(path); }

    public boolean isClean() {
        return modifiedFiles.isEmpty() && deletedFiles.isEmpty() && untrackedFiles.isEmpty();
    }

    public List<String> getIntactFiles() { return Collections.unmodifiableList(intactFiles); }
    public List<String> getModifiedFiles() { return Collections.unmodifiableList(modifiedFiles); }
    public List<String> getDeletedFiles() { return Collections.unmodifiableList(deletedFiles); }
    public List<String> getUntrackedFiles() { return Collections.unmodifiableList(untrackedFiles); }

    public void printConsoleSummary() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("==================================================");
        System.out.println("        FILEGUARD INTEGRITY AUDIT REPORT          ");
        System.out.println("==================================================");
        System.out.println("Execution Time: " + timestamp.format(dtf));
        System.out.println("Total Files Evaluated: " + (intactFiles.size() + modifiedFiles.size() + deletedFiles.size()));
        System.out.println("--------------------------------------------------");
        System.out.println(" [OK] Intact Files      : " + intactFiles.size());
        System.out.println(" [!]  Modified Files    : " + modifiedFiles.size());
        System.out.println(" [-]  Deleted Files     : " + deletedFiles.size());
        System.out.println(" [+]  Untracked (New)   : " + untrackedFiles.size());
        System.out.println("--------------------------------------------------");

        if (isClean()) {
            System.out.println("RESULT: [PASSED] System directory integrity verified.");
        } else {
            System.out.println("RESULT: [ALERT] Integrity discrepancies detected!");
            if (!modifiedFiles.isEmpty()) {
                System.out.println("\nModified Files (Checksum mismatch):");
                for (String f : modifiedFiles) System.out.println("  * " + f);
            }
            if (!deletedFiles.isEmpty()) {
                System.out.println("\nDeleted Files (Missing from baseline):");
                for (String f : deletedFiles) System.out.println("  * " + f);
            }
            if (!untrackedFiles.isEmpty()) {
                System.out.println("\nUntracked New Files:");
                for (String f : untrackedFiles) System.out.println("  * " + f);
            }
        }
        System.out.println("==================================================");
    }
}
