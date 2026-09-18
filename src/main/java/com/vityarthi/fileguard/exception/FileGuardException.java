package com.vityarthi.fileguard.exception;

public class FileGuardException extends Exception {
    private static final long serialVersionUID = 1L;

    public FileGuardException(String message) {
        super(message);
    }

    public FileGuardException(String message, Throwable cause) {
        super(message, cause);
    }
}
