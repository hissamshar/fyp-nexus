package com.fyp.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton HikariCP connection pool.
 * Loaded from config.properties on the classpath.
 */
public class DBConnection {

    private static HikariDataSource dataSource;

    static {
        try {
            Properties props = loadProperties();
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.user"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("FYP-HikariPool");

            // Schema path required for fyp schema
            config.setSchema("fyp");

            dataSource = new HikariDataSource(config);
            System.out.println("[DBConnection] Pool initialised successfully.");
        } catch (Exception e) {
            System.err.println("[DBConnection] FATAL: Could not initialise DB pool: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {}

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();

        // Try loading from file system first (for production)
        java.io.File file = new java.io.File("config.properties");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                props.load(fis);
                return props;
            }
        }

        // Fallback: classpath
        try (InputStream is = DBConnection.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is == null) throw new IOException("config.properties not found on classpath or working directory.");
            props.load(is);
        }
        return props;
    }

    /**
     * Get a connection from the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialised.");
        }
        return dataSource.getConnection();
    }

    /**
     * Gracefully shut down the pool (call on application exit).
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DBConnection] Pool shut down.");
        }
    }
}
