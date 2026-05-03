package com.fyp.util;

import java.util.regex.Pattern;

/**
 * Password strength validation.
 * With Supabase Auth handling hashing, this class only validates strength.
 */
public class PasswordUtil {

    // Regex: min 8 chars, upper, lower, digit, special char
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()\\-+=])[A-Za-z\\d@$!%*?&_#^()\\-+=]{8,}$"
    );

    private PasswordUtil() {}

    /**
     * @deprecated Supabase Auth handles password hashing. Kept as stub.
     */
    public static String hash(String plain) {
        // Supabase handles password hashing server-side
        return plain;
    }

    /**
     * @deprecated Supabase Auth handles password verification. Kept as stub.
     */
    public static boolean verify(String plain, String hashed) {
        // Supabase handles password verification server-side
        return plain != null && plain.equals(hashed);
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
