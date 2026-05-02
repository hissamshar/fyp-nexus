package com.fyp.util;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generate, store, and verify 6-digit OTPs with 15-minute expiry.
 * Stored in fyp.otp_tokens. OTPs are deleted immediately after verification.
 */
public class OTPService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 15;

    private OTPService() {}

    /**
     * Generate a 6-digit OTP, store it in the DB, and return the code.
     * @param userId  The user who needs the OTP
     * @param purpose "EMAIL_VERIFY" or "PASSWORD_RESET"
     */
    public static String generateAndStore(UUID userId, String purpose) throws Exception {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        // Invalidate any existing unused OTPs for this user + purpose
        String invalidateSql = "UPDATE fyp.otp_tokens SET is_used = TRUE " +
                               "WHERE user_id = ?::uuid AND purpose = ? AND is_used = FALSE";

        String insertSql = "INSERT INTO fyp.otp_tokens (user_id, otp_code, purpose, expires_at) " +
                           "VALUES (?::uuid, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(invalidateSql)) {
                ps.setString(1, userId.toString());
                ps.setString(2, purpose);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, userId.toString());
                ps.setString(2, otp);
                ps.setString(3, purpose);
                ps.setObject(4, expiresAt);
                ps.executeUpdate();
            }
        }

        return otp;
    }

    /**
     * Verify an OTP. Returns true if valid, not expired, and not already used.
     * Deletes the token immediately on successful verification.
     */
    public static boolean verify(UUID userId, String inputOtp, String purpose) throws Exception {
        String sql = "SELECT token_id, expires_at FROM fyp.otp_tokens " +
                     "WHERE user_id = ?::uuid AND otp_code = ? AND purpose = ? " +
                     "AND is_used = FALSE ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, inputOtp);
            ps.setString(3, purpose);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;  // Not found

                UUID tokenId  = UUID.fromString(rs.getString("token_id"));
                LocalDateTime expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();

                if (LocalDateTime.now().isAfter(expiresAt)) {
                    // Expired — mark as used but don't grant access
                    markUsed(conn, tokenId);
                    return false;
                }

                // Valid — delete token immediately
                markUsed(conn, tokenId);
                return true;
            }
        }
    }

    private static void markUsed(Connection conn, UUID tokenId) throws Exception {
        String sql = "UPDATE fyp.otp_tokens SET is_used = TRUE WHERE token_id = ?::uuid";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenId.toString());
            ps.executeUpdate();
        }
    }
}
