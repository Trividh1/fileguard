# PROJECT REPORT

**Course Code:** CSE2006  
**Course Name:** Programming in Java  
**Project Title:** FileGuard: Cryptographic File Integrity & Audit Monitor  
**Student Name:** Trividh  
**Institution:** VIT Bhopal University  
**Academic Year:** 2026  

---

## 1. Cover Page
* **Project Name:** FileGuard
* **Domain:** System Utilities, Cryptographic Security & Digital Forensics
* **Course:** CSE2006 – Programming in Java
* **Author:** Trividh
* **Platform/Environment:** Java Standard Edition (JDK 17 LTS), Command-Line Interface (CLI)

---

## 2. Introduction
Data integrity is one of the fundamental pillars of information security (alongside confidentiality and availability). In modern operating environments, unauthorized alterations to critical files—whether by malware, malicious insider actions, or accidental corruption—can compromise system reliability, invalidate build pipelines, and introduce severe security vulnerabilities.

`FileGuard` is a high-performance, command-line file integrity monitoring and audit system developed in Java. It allows system administrators, developers, and forensic investigators to baseline a directory tree cryptographically, audit deviations at any point in time, and execute real-time background file observation using Java's Native I/O (`java.nio.file.WatchService`). Designed to have zero third-party runtime dependencies, the project applies object-oriented programming (OOP), the Java Collections Framework, multi-threading, streaming I/O, custom exception handling, and automated unit testing.

---

## 3. Problem Statement
Modern production systems frequently suffer from "silent corruption" and unauthorized file alterations:
1. **Unmonitored Configuration Drift**: Configuration files in `/etc` or application properties are altered without an audit trail.
2. **Web Shells and Injected Backdoors**: Attackers often introduce rogue scripts (`.sh`, `.jsp`, `.php`) into server directories.
3. **Absence of Lightweight Auditing**: Most enterprise integrity systems (like OSSEC or Tripwire) require heavy background daemons, complex server-agent setups, and extensive configuration overhead.

There is a distinct need for a lightweight, terminal-executable Java tool capable of computing cryptographic digests, persisting state baselines, running high-speed delta audits, and monitoring live changes via standard terminal commands.

---

## 4. Functional Requirements
`FileGuard` incorporates three primary functional modules:

### Module 1: Cryptographic Baseline Engine
* **Directory Traversal**: Recursively scans directory trees using `Files.walk()`.
* **Digest Generation**: Computes SHA-256 cryptographic hashes for every file using chunked 8 KB buffered streams.
* **Manifest Persistence**: Serializes file metadata (relative path, checksum, byte size, epoch modification timestamp) into `.fileguard_baseline.manifest`.

### Module 2: State Verification & Audit Engine
* **Delta Comparison**: Loads the baseline manifest and compares it against active file system states.
* **Discrepancy Categorization**: Classifies every file as:
  * `INTACT`: Identical SHA-256 checksum.
  * `MODIFIED`: Checksum mismatch (payload altered).
  * `DELETED`: Present in baseline but removed from active file system.
  * `UNTRACKED_NEW`: Present in active file system but absent in baseline.
* **Exit Code Signaling**: Emits exit code `0` on clean state and `2` on tamper detection for CI/CD scripting.

### Module 3: Real-Time Live Watch Service
* **Background Threading**: Implements `Runnable` on a dedicated monitoring thread.
* **NIO Event Dispatch**: Registers directory watch keys with `StandardWatchEventKinds` (`ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`).
* **Instant Alerting**: Inspects real-time events, computes delta digests on modified files, and emits timestamped alerts to standard output.

---

## 5. Non-Functional Requirements
1. **Security & Cryptographic Robustness**: Relies on the standard cryptographic SHA-256 algorithm via `MessageDigest` (256-bit hash resistance against collisions and pre-image attacks).
2. **Resource Efficiency & Memory Scalability**: Uses an 8 KB chunk buffer (`BufferedInputStream`) during hashing rather than loading full files into memory, allowing processing of multi-gigabyte files with negligible JVM heap consumption.
3. **Reliability & Exception Safety**: Implements a custom checked exception hierarchy (`FileGuardException`), guaranteeing graceful failure handling during file permission restrictions, missing paths, and concurrent locks.
4. **Maintainability & Portability**: Adheres strictly to standard Java SE APIs without external runtime libraries, ensuring cross-platform execution on Linux, macOS, and Windows.

---

## 6. System Architecture

```
+-------------------------------------------------------------+
|                      User CLI Interface                     |
|           (FileGuardApp - Command-Line Parser)              |
+------------------------------+------------------------------+
                               |
            +------------------+------------------+
            |                  |                  |
            v                  v                  v
     [ init command ]   [ check command ]  [ watch command ]
            |                  |                  |
            v                  v                  v
+-----------------------+ +--------------------+ +-------------------+
|    BaselineEngine     | | VerificationEngine | |    LiveMonitor    |
| - Directory Traversal | | - Delta Evaluation | | - Multi-threading |
| - SHA-256 Hashing     | | - Status Matching  | | - WatchService    |
+-----------+-----------+ +---------+----------+ +---------+---------+
            |                       |                      |
            +-----------------------+----------------------+
                                    |
                                    v
            +---------------------------------------------+
            |              Core Hasher Utility            |
            |     (MessageDigest, BufferedInputStream)    |
            +----------------------+----------------------+
                                   |
                                   v
            +---------------------------------------------+
            |              Target Filesystem              |
            |      (.fileguard_baseline.manifest)         |
            +---------------------------------------------+
```

---

## 7. Design Diagrams

### 7.1 Use Case Diagram
```
+---------------------------------------------------------------+
|                       FileGuard System                        |
|                                                               |
|   (User / Sysadmin)                                           |
|          |                                                    |
|          +----> [ UC1: Generate Baseline Manifest ]           |
|          |                                                    |
|          +----> [ UC2: Run Point-in-Time Audit ]              |
|          |               |                                    |
|          |               +--<<include>>--> [ Compute SHA256 ] |
|          |                                                    |
|          +----> [ UC3: Monitor Directory in Real-Time ]       |
|          |                                                    |
|          +----> [ UC4: Inspect Tracked File Statuses ]        |
+---------------------------------------------------------------+
```

### 7.2 Workflow Diagram
```
[Start CLI] ---> [Parse Arguments]
                     |
        +------------+------------+
        |            |            |
     ("init")     ("check")    ("watch")
        |            |            |
        v            v            v
  [Walk Files]  [Load Manifest] [Load Manifest]
        |            |            |
  [Hash Files]  [Hash Active]   [Spawn Thread]
        |            |            |
  [Save Manifest] [Compare Sets] [Register NIO Keys]
        |            |            |
     [Finish]   [Print Report]  [Event Loop (Take/Poll)]
                     |            |
               [Exit 0 / 2]     [Emit Alerts]
```

### 7.3 Sequence Diagram: Verification Audit (`check`)
```
User            FileGuardApp        VerificationEngine        Hasher         Filesystem
 |                   |                      |                    |                |
 |-- run check ----->|                      |                    |                |
 |                   |-- verify(dir) ------>|                    |                |
 |                   |                      |-- loadManifest() ------------------>|
 |                   |                      |<-- baselineMap ---------------------|
 |                   |                      |-- walk & scan current ------------->|
 |                   |                      |<-- activePaths ---------------------|
 |                   |                      |                    |                |
 |                   |                      |-- computeSHA256 ->|                |
 |                   |                      |   for each file   |-- readBytes -->|
 |                   |                      |                   |<-- rawBytes ---|
 |                   |                      |<-- hexDigest ------|                |
 |                   |                      |                    |                |
 |                   |                      |-- evaluate deltas                   |
 |                   |<-- IntegrityReport --|                                     |
 |                   |                                                            |
 |<-- Print Report --|                                                            |
```

### 7.4 Class / Component Diagram
* **`FileGuardApp`**: Entry point handling CLI options (`init`, `check`, `watch`, `status`).
* **`BaselineEngine`**: Handles directory scanning and manifest file serialization.
* **`VerificationEngine`**: Compares in-memory baseline state against disk reality.
* **`LiveMonitor`**: Implements `Runnable`, wraps `java.nio.file.WatchService`.
* **`Hasher`**: Thread-safe static utility for streaming SHA-256 generation.
* **`FileRecord`**: Value object modeling file path, checksum, size, and timestamp.
* **`IntegrityReport`**: Data carrier holding intact, modified, deleted, and untracked file collections.
* **`FileGuardException`**: Checked exception defining error boundaries.

### 7.5 Manifest Storage Schema
The manifest file (`.fileguard_baseline.manifest`) uses comma-separated formatting:
```
<relative_path>,<sha256_hash>,<file_size_bytes>,<last_modified_timestamp>
```
* **relative_path** (`String`): Normalized relative path from directory root.
* **sha256_hash** (`String[64]`): 64-character lowercase hex digest.
* **file_size_bytes** (`long`): Total file size in bytes.
* **last_modified_timestamp** (`long`): Epoch millisecond timestamp.

---

## 8. Design Decisions & Rationale
1. **Choice of SHA-256 over MD5/SHA-1**: MD5 and SHA-1 suffer from well-documented collision vulnerabilities. SHA-256 provides cryptographic certainty against adversarial forgery while executing efficiently on modern hardware.
2. **Chunked Streaming Hashing (8 KB Buffer)**: Reading entire files via `Files.readAllBytes()` leads to `OutOfMemoryError` on large video or database files. Using `BufferedInputStream` with an 8 KB buffer limits memory consumption to constant time $O(1)$.
3. **CSV Manifest Representation**: Chosen over JSON or XML to eliminate third-party dependencies (like Jackson or Gson), ensuring clean compilation with the base JDK while maintaining human-readable persistence.
4. **Decoupled Engine Architecture**: Separating manifest generation (`BaselineEngine`) from audit verification (`VerificationEngine`) and live monitoring (`LiveMonitor`) enables isolated unit testing and modular reusability.

---

## 9. Implementation Details
The project was constructed across 10 classes in the `com.vityarthi.fileguard` package:
* **Object-Oriented Encapsulation**: `FileRecord` encapsulates file properties and exposes strict getters, preventing unauthorized state modification.
* **Java Streams and NIO.2**: `Files.walk()` and `Path.relativize()` enable clean directory traversal across platform file separators.
* **Multi-threading and Concurrency**: `LiveMonitor` runs as an asynchronous background worker implementing `Runnable`, interacting safely with the host operating system's native file notification APIs.
* **Defensive Resource Management**: All file handlers use `try-with-resources` blocks to prevent file descriptor leaks.

---

## 10. Screenshots & Execution Results
*(Insert terminal screenshots showing the following command sequences)*

### Sample 1: Baseline Initialization
```bash
$ java -jar target/fileguard-1.0.0.jar init ./data_dir
Initializing integrity baseline for: /home/student/data_dir
Successfully indexed 3 files.
Baseline manifest stored at: /home/student/data_dir/.fileguard_baseline.manifest
```

### Sample 2: Clean Verification Audit
```bash
$ java -jar target/fileguard-1.0.0.jar check ./data_dir
Auditing integrity for: /home/student/data_dir
==================================================
        FILEGUARD INTEGRITY AUDIT REPORT          
==================================================
Execution Time: 2026-09-18 14:40:12
Total Files Evaluated: 3
--------------------------------------------------
 [OK] Intact Files      : 3
 [!]  Modified Files    : 0
 [-]  Deleted Files     : 0
 [+]  Untracked (New)   : 0
--------------------------------------------------
RESULT: [PASSED] System directory integrity verified.
==================================================
```

### Sample 3: Tampering Detection Audit
```bash
$ echo "malicious payload" >> ./data_dir/config.json
$ rm ./data_dir/secret.key
$ touch ./data_dir/unauthorized.sh

$ java -jar target/fileguard-1.0.0.jar check ./data_dir
Auditing integrity for: /home/student/data_dir
==================================================
        FILEGUARD INTEGRITY AUDIT REPORT          
==================================================
Execution Time: 2026-09-18 14:42:50
Total Files Evaluated: 3
--------------------------------------------------
 [OK] Intact Files      : 1
 [!]  Modified Files    : 1
 [-]  Deleted Files     : 1
 [+]  Untracked (New)   : 1
--------------------------------------------------
RESULT: [ALERT] Integrity discrepancies detected!

Modified Files (Checksum mismatch):
  * config.json

Deleted Files (Missing from baseline):
  * secret.key

Untracked New Files:
  * unauthorized.sh
==================================================
```

---

## 11. Testing Approach
Automated testing is implemented using **JUnit 5 (Jupiter)** in `src/test/java/com/vityarthi/fileguard/FileGuardTest.java`. The test suite uses temporary directories (`@TempDir`) to ensure sandboxed test execution.

| Test Case ID | Target Component | Input / Scenario | Expected Output | Status |
|---|---|---|---|---|
| **TC-01** | `Hasher` | 2 identical strings hashed independently | Both yield identical 64-char SHA-256 strings | Pass |
| **TC-02** | `BaselineEngine` | Directory with 2 new files | Manifest contains exactly 2 records | Pass |
| **TC-03** | `VerificationEngine` | Unaltered directory | `report.isClean() == true`, 0 modified/deleted | Pass |
| **TC-04** | `VerificationEngine` | Modifying content of 1 file | `report.isClean() == false`, 1 file flagged in modified list | Pass |
| **TC-05** | `VerificationEngine` | Deleting 1 file from directory | 1 file identified in deleted list | Pass |
| **TC-06** | `FileGuardApp` | Invalid directory path argument | Exits gracefully with exit code 1 and error message | Pass |

---

## 12. Challenges Faced
1. **Self-Referential Baseline Invalidation**: When generating the manifest file inside the root directory, subsequent runs flagged `.fileguard_baseline.manifest` as an untracked new file.
   * *Resolution*: Added filter exclusions (`!p.getFileName().equals(MANIFEST_FILE_NAME)`) across both baseline generation and verification engines.
2. **File Locking During Live Monitoring**: On operating systems like Windows, modifying a file fires multiple `ENTRY_MODIFY` events in rapid succession while the file is still locked by the editing editor.
   * *Resolution*: Wrapped live checksum calculation inside a guarded try-catch block to ignore transient I/O locks and allow clean subsequent reads.
3. **Platform Path Separator Incompatibilities**: Windows uses backslashes (`\`) while Linux/UNIX uses forward slashes (`/`).
   * *Resolution*: Standardized relative paths using `Path.relativize()` and normalized paths before persistence.

---

## 13. Learnings & Key Takeaways
* In-depth understanding of the `java.nio.file` package, including the power of `WatchService` for reactive OS file event handling.
* Hands-on experience applying cryptographic primitives (`MessageDigest`) and streaming data buffers to balance security with CPU/memory performance.
* Practical mastery of the Java Collections Framework (`HashMap`, `ArrayList`) for managing in-memory delta comparisons.
* Integration of JUnit 5 temporary directory abstractions for deterministic file-system testing.

---

## 14. Future Enhancements
* **Configurable Hash Algorithms**: Allow CLI flags to toggle between SHA-256, SHA-512, and BLAKE3.
* **GPG/Digital Signature on Manifests**: Sign the baseline manifest with an RSA/ECDSA private key to prevent attackers from regenerating the baseline after tampering.
* **Automated Rollback Engine**: Cache verified file snapshots to allow single-command restoration upon tampering detection.
* **Webhook & Email Alerting**: Dispatch alerts to Discord, Slack, or SMTP endpoints upon live tampering events.

---

## 15. References
1. Oracle Corporation. (2023). *Java Platform, Standard Edition Documentation (JDK 17)*. Oracle Technology Network.
2. Bloch, J. (2018). *Effective Java (3rd Edition)*. Addison-Wesley Professional.
3. National Institute of Standards and Technology (NIST). (2015). *FIPS PUB 180-4: Secure Hash Standard (SHS)*. U.S. Department of Commerce.
4. Goetz, B., Peierls, T., Bloch, J., Bowbeer, J., Holmes, D., & Lea, D. (2006). *Java Concurrency in Practice*. Addison-Wesley Professional.
