# Project Statement: FileGuard

## 1. Problem Statement
In modern computing systems and shared developer environments, critical directories (such as configuration repositories, web server document roots, and build artifact folders) are vulnerable to unauthorized alterations, silent file corruption, and malicious tampering. System administrators and developers often lack a lightweight, dependency-free command-line tool capable of creating verifiable cryptographic baselines and auditing file state deviations on demand or in real-time.

## 2. Scope of the Project
`FileGuard` is a standalone, terminal-based Java application designed to monitor directory integrity. The scope includes:
* Recursive scanning of arbitrary directory trees to record file sizes, timestamps, and SHA-256 cryptographic digests.
* Persistence of baseline state into structured manifest files.
* Point-in-time integrity verification comparing active file system states against stored baselines to detect modified, deleted, and untracked files.
* Real-time background directory watching leveraging Java NIO `WatchService` for instant tampering detection.
* Automated exit status signaling suitable for CI/CD pipelines and shell scripting integration.

Out of Scope: Remote network synchronization, distributed cluster telemetry, and automated file rollbacks.

## 3. Target Users
* **System Administrators**: Requiring lightweight validation of critical `/etc` configuration folders without bloated agent installations.
* **Software Developers & DevOps Engineers**: Integrating file integrity gates into automated deployment and build verification pipelines.
* **Forensic Analysts & Students**: Auditing evidence directories and studying cryptographic hashing and file system APIs in Java.

## 4. High-Level Features
* **Cryptographic Baselines**: Fast, chunked SHA-256 calculation for directories of arbitrary size.
* **Point-in-Time Audit**: Clean visual summary categorized by intact, modified, deleted, and untracked files.
* **Live Tamper Monitoring**: Multi-threaded real-time event listener logging unauthorized modifications.
* **Zero External Runtime Dependencies**: Implemented strictly using the Java Standard Edition Library (Java SE 17).
