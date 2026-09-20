package de.jawollo07.siedler.storage;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.utils.Config;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

public final class StorageManager {

    private final SiedlerPlugin plugin;
    private Connection connection;
    private SQLite sqlite;
    private MySQL mysql;
    private String activeType;

    public StorageManager(SiedlerPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public synchronized void initialize() {
        close();

        Config config = new Config(new File(plugin.getDataFolder(), "config.yml"), Config.YAML);
        String configuredType = config.getString("storage.type", "sqlite");
        String storageType = configuredType == null ? "sqlite" : configuredType.trim().toLowerCase(Locale.ROOT);
        boolean initialized = false;

        try {
            if ("mysql".equals(storageType) || "mariadb".equals(storageType)) {
                mysql = new MySQL(config);
                mysql.connect();
                connection = mysql.getConnection();
                activeType = "mariadb";
                plugin.getLogger().info("MariaDB storage initialized.");
            } else if ("sqlite".equals(storageType)) {
                sqlite = new SQLite(plugin);
                sqlite.initialize();
                connection = sqlite.getConnection();
                activeType = "sqlite";
            } else {
                throw new IllegalArgumentException(
                        "Unsupported storage type '" + storageType + "'. Supported types: sqlite, mariadb"
                );
            }

            new InitDB().initDatabase();
            plugin.getLogger().info("Database schema ensured and upgraded automatically.");
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize or upgrade the database schema", e);
        } finally {
            if (!initialized) {
                close();
            }
        }
    }

    public synchronized Connection getConnection() {
        if (connection == null || isClosed(connection)) {
            throw new IllegalStateException("Storage is not initialized");
        }
        return connection;
    }

    public synchronized void close() {
        if (connection == null && mysql == null && sqlite == null) {
            return;
        }
        try {
            if (mysql != null) {
                mysql.close();
            } else if (sqlite != null) {
                sqlite.close();
            } else {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not close storage: " + e.getMessage());
        } finally {
            connection = null;
            mysql = null;
            sqlite = null;
            activeType = null;
        }
    }

    public synchronized boolean isInitialized() {
        return connection != null && !isClosed(connection);
    }

    public synchronized String getActiveType() {
        return activeType;
    }

    private boolean isClosed(Connection connection) {
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}
