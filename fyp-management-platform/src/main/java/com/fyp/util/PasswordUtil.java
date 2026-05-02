package com.fyp.util;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;

/**
 * BCrypt password hashing and strength validation.
 */
public class PasswordUtil {

    // Regex: min 8 chars, upper, lower, digit, special char
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()\\-+=])[A-Za-z\\d@$!%*?&_#^()\\-+=]{8,}$"
    );

    private PasswordUtil() {}

    /**
     * Hash a plain-text password using BCrypt with cost factor 12.
     */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /**
     * Verify a plain-text password against a stored BCrypt hash.
     */
    public static boolean verify(String plain, String hashed) {
        if (plain == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a password meets strength requirements:
     * min 8 chars, at least one uppercase, lowercase, digit, and special character.
     */
    public static boolean isStrong(String password) {
        if (password == null) return false;
        return STRONG_PASSWORD.matcher(password).matches();
    }

    /**
     * Returns a human-readable reason if the password is weak.
     */
    public static String getWeaknessReason(String password) {
        if (password == null || password.length() < 8)
            return "Password must be at least 8 characters.";
        if (!password.chars().anyMatch(Character::isUpperCase))
            return "Password must contain at least one uppercase letter.";
        if (!password.chars().anyMatch(Character::isLowerCase))
            return "Password must contain at least one lowercase letter.";
        if (!password.chars().anyMatch(Character::isDigit))
            return "Password must contain at least one digit.";
        if (password.chars().noneMatch(c -> "@$!%*?&_#^()-+=".indexOf(c) >= 0))
            return "Password must contain at least one special character (@$!%*?&_#^()-+=).";
        return null;
    }
}
