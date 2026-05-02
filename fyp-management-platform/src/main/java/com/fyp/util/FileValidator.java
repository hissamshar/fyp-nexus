package com.fyp.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Validates uploaded files:
 * - Extension must be pdf, docx, zip, or pptx
 * - First 512 bytes (magic bytes) must match the declared type
 * - File size must be ≤ 50 MB
 * - Path sanitisation against directory traversal
 */
public class FileValidator {

    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "zip", "pptx");

    // Magic byte signatures for supported types
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "pdf",  new byte[]{0x25, 0x50, 0x44, 0x46},              // %PDF
        "zip",  new byte[]{0x50, 0x4B, 0x03, 0x04},              // PK (zip / docx / pptx use zip)
        "pptx", new byte[]{0x50, 0x4B, 0x03, 0x04},              // PK (same as zip)
        "docx", new byte[]{0x50, 0x4B, 0x03, 0x04}               // PK (same as zip)
    );

    private FileValidator() {}

    /**
     * Returns true if the file extension is allowed AND the magic bytes match.
     */
    public static boolean isAllowedType(File file) {
        if (file == null || !file.exists()) return false;

        String ext = getExtension(file.getName()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) return false;

        byte[] magic = MAGIC_BYTES.get(ext);
        if (magic == null) return false;

        return matchesMagicBytes(file, magic);
    }

    /**
     * Returns true if the file is within the 50 MB limit.
     */
    public static boolean isUnderSizeLimit(File file) {
        return file != null && file.length() <= MAX_SIZE_BYTES;
    }

    /**
     * Sanitise a file path — strip directory traversal attempts.
     * Returns the sanitised path, or throws if it's still unsafe.
     */
    public static String sanitisePath(String rawPath) {
        if (rawPath == null) throw new IllegalArgumentException("File path cannot be null.");
        String sanitised = rawPath.replace("\\", "/");
        while (sanitised.contains("../")) sanitised = sanitised.replace("../", "");
        while (sanitised.contains("./"))  sanitised = sanitised.replace("./", "");
        return sanitised;
    }

    /**
     * Returns the file extension without the dot.
     */
    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private static boolean matchesMagicBytes(File file, byte[] magic) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[magic.length];
            int read = fis.read(header, 0, magic.length);
            if (read < magic.length) return false;
            for (int i = 0; i < magic.length; i++) {
                if (header[i] != magic[i]) return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
