package com.example.examseating.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FlexiblePasswordEncoderTest {

    private FlexiblePasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new FlexiblePasswordEncoder();
    }

    @Test
    void testExactMatch() {
        String hash = encoder.encode("admin123");
        assertTrue(encoder.matches("admin123", hash));
        assertFalse(encoder.matches("wrongpassword", hash));
    }

    @Test
    void testCaseInsensitiveRegisterNumberMatch() {
        // When student is seeded with lowercase username "2024cse001"
        String hash = encoder.encode("2024cse001");

        // Lowercase matches
        assertTrue(encoder.matches("2024cse001", hash));

        // Uppercase register number matches
        assertTrue(encoder.matches("2024CSE001", hash));

        // Mixed case matches
        assertTrue(encoder.matches("2024Cse001", hash));
    }

    @Test
    void testWhitespaceTrimmedMatch() {
        String hash = encoder.encode("2024cse001");

        // Accidental trailing space from mobile keyboard
        assertTrue(encoder.matches("2024cse001 ", hash));

        // Accidental leading space
        assertTrue(encoder.matches(" 2024CSE001 ", hash));
    }

    @Test
    void testNullAndEmptyHandling() {
        String hash = encoder.encode("password");
        assertFalse(encoder.matches(null, hash));
        assertFalse(encoder.matches("", hash));
        assertFalse(encoder.matches("password", null));
    }
}
