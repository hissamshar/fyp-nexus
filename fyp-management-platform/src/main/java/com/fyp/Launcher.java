package com.fyp;

/**
 * Proxy launcher class to bypass JavaFX module checks when running as a Fat JAR
 * or Native Executable. This class MUST NOT extend Application.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
