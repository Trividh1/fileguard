package com.vityarthi.fileguard.core;

import com.vityarthi.fileguard.exception.FileGuardException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hasher {
    private static final String HASH_ALGO = "SHA-256";
    private static final int BUFFER_SIZE = 8192; // 8 KB chunk buffer

    private Hasher() {}

    public static String computeSHA256(Path path) throws FileGuardException {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
            try (InputStream fis = new BufferedInputStream(new FileInputStream(path.toFile()), BUFFER_SIZE)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new FileGuardException("Failed to calculate SHA-256 for: " + path + " - " + e.getMessage(), e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
