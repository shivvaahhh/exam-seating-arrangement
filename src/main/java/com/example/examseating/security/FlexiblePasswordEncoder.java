package com.example.examseating.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Flexible BCrypt Password Encoder that handles real-world login conveniences:
 * 1. Standard BCrypt matching (exact match).
 * 2. Trimmed password matching (protects against trailing/leading spaces from mobile autofill or copy-paste).
 * 3. Case-insensitive fallback for student register numbers (e.g., student typing "2024CSE001" when initial password is "2024cse001").
 */
public class FlexiblePasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // 1. Exact match (standard case-sensitive BCrypt comparison)
        if (delegate.matches(rawPassword, encodedPassword)) {
            return true;
        }

        String rawStr = rawPassword.toString();

        // 2. Trimmed match (prevents failures from accidental trailing/leading spaces)
        String trimmed = rawStr.trim();
        if (!trimmed.equals(rawStr) && delegate.matches(trimmed, encodedPassword)) {
            return true;
        }

        // 3. Lowercase trimmed match (allows students to type their register number in uppercase e.g. 2024CSE001)
        String lowerTrimmed = trimmed.toLowerCase();
        if (!lowerTrimmed.equals(trimmed) && delegate.matches(lowerTrimmed, encodedPassword)) {
            return true;
        }

        // 4. Uppercase trimmed match
        String upperTrimmed = trimmed.toUpperCase();
        if (!upperTrimmed.equals(trimmed) && delegate.matches(upperTrimmed, encodedPassword)) {
            return true;
        }

        return false;
    }
}
