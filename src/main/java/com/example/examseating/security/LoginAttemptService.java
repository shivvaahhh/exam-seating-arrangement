package com.example.examseating.security;

import org.springframework.stereotype.Service;

/**
 * Login attempt service. Lockout disabled for demo/academic project convenience.
 */
@Service
public class LoginAttemptService {

    public void loginSucceeded(String key) {
        // No-op: Lockout disabled
    }

    public void loginFailed(String key) {
        // No-op: Lockout disabled for project demo
    }

    public boolean isBlocked(String key) {
        return false; // Never lock out users
    }

    public long getRemainingLockoutMinutes(String key) {
        return 0;
    }
}
