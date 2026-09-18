# FileGuard: Cryptographic File Integrity & Audit Monitor

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

A robust, dependency-free command-line file integrity monitoring and audit system built with Java 17. Designed as part of the **CSE2006: Programming in Java** course evaluation.

---

## Table of Contents
1. [Overview](#overview)
2. [Key Features](#key-features)
3. [Architecture & Technologies](#architecture--technologies)
4. [Prerequisites](#prerequisites)
5. [Setup & Installation](#setup--installation)
6. [Usage Guide](#usage-guide)
7. [Running Tests](#running-tests)
8. [Project Layout](#project-layout)

---

## Overview
`FileGuard` provides system administrators and developers with a simple yet powerful CLI to establish cryptographic baselines for directory trees, detect unauthorized modifications or deletions, and continuously monitor directories for real-time tampering events.

---

## Key Features
* **Deterministic Cryptographic Hashing**: Uses SHA-256 with 8 KB buffered streams for low memory overhead.
* **Three Functional Modules**:
  * *Baseline Manifest Engine*: Indexes directory hierarchies into persistent manifests.
  * *Integrity Verification Engine*: Detects file modifications, deletions, and newly created files.
  * *Live Monitor Service*: Leverages multi-threaded Java NIO `WatchService` for real-time alerting.
* **UNIX Exit Codes**: Returns `0` on clean integrity checks and `2` on detected tampering, allowing seamless CI/CD shell script automation.
* **Zero External Dependencies**: Pure Java SE standard library.

---

## Architecture & Technologies
* **Language**: Java 17 (LTS)
* **Core APIs**: `java.nio.file`, `java.security.MessageDigest`, `java.util.concurrent`, `java.io`
* **Build System**: Maven (or standard `javac`)
* **Testing**: JUnit 5 (Jupiter)

---

## Prerequisites
* Java Development Kit (JDK) 17 or higher
* Apache Maven 3.8+ (Optional; standard `javac` can also be used)

Check your environment:
```bash
java -version
javac -version
mvn -version
```

---

## Setup & Installation

### Option A: Using Maven (Recommended)
```bash
# Clone the repository
git clone https://github.com/<username>/fileguard.git
cd fileguard

# Compile and package into an executable JAR
mvn clean package

# The generated JAR will be at target/fileguard-1.0.0.jar
```

### Option B: Using Standard `javac`
```bash
# Create bin directory
mkdir -p bin

# Compile all source files
javac -d bin src/main/java/com/vityarthi/fileguard/*.java \
           src/main/java/com/vityarthi/fileguard/model/*.java \
           src/main/java/com/vityarthi/fileguard/core/*.java \
           src/main/java/com/vityarthi/fileguard/exception/*.java
```

---

## Usage Guide

Run using the packaged JAR:
```bash
java -jar target/fileguard-1.0.0.jar <command> <target_directory>
```
*(Or via compiled class files: `java -cp bin com.vityarthi.fileguard.FileGuardApp <command> <target_directory>`)*

### 1. Initialize Baseline Manifest (`init`)
Scans the specified directory and writes `.fileguard_baseline.manifest`:
```bash
java -jar target/fileguard-1.0.0.jar init ./my_data
```

### 2. Verify Directory Integrity (`check`)
Audits all files against the baseline:
```bash
java -jar target/fileguard-1.0.0.jar check ./my_data
```

### 3. Continuous Real-Time Monitoring (`watch`)
Starts a background monitoring thread that alerts on file events:
```bash
java -jar target/fileguard-1.0.0.jar watch ./my_data
```

---

## Running Tests
Execute the unit test suite via Maven:
```bash
mvn test
```

---

## Project Layout
```
fileguard/
├── pom.xml
├── README.md
├── statement.md
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── vityarthi/
    │               └── fileguard/
    │                   ├── FileGuardApp.java          # CLI Main Entry Point
    │                   ├── core/
    │                   │   ├── BaselineEngine.java    # Module 1: Manifest generation
    │                   │   ├── VerificationEngine.java# Module 2: State auditing
    │                   │   ├── LiveMonitor.java       # Module 3: Real-time watcher
    │                   │   └── Hasher.java            # Cryptographic hashing utility
    │                   ├── model/
    │                   │   ├── FileRecord.java        # POJO for file metadata & digest
    │                   │   ├── IntegrityReport.java   # Audit outcome representation
    │                   │   └── IntegrityStatus.java   # Verification status enum
    │                   └── exception/
    │                       └── FileGuardException.java# Custom checked exception
    └── test/
        └── java/
            └── com/
                └── vityarthi/
                    └── fileguard/
                        └── FileGuardTest.java         # JUnit 5 test suite
```

---

## License
Distributed under the MIT License.
