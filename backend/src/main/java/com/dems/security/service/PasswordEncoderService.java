package com.dems.security.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncoderService {

    private static final int PARALLELISM = 1;
    private static final int MEMORY = 65536;
    private static final int ITERATIONS = 3;

    private final Argon2 argon2;

    public PasswordEncoderService() {
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);
    }

    public String encode(char[] password) {
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password);
        } finally {
            argon2.wipeArray(password);
        }
    }

    public String encode(String rawPassword) {
        char[] passwordChars = rawPassword.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }

    public boolean matches(String rawPassword, String encodedHash) {
        if (rawPassword == null || encodedHash == null) {
            return false;
        }
        char[] passwordChars = rawPassword.toCharArray();
        try {
            return argon2.verify(encodedHash, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }
}
