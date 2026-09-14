package de.jawollo07.siedler.storage;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.utils.Config;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

public final class StorageManager {

    private final SiedlerPlugin plugin;
    private Connection connection;
    private SQLite sqlite;
    private MySQL mysql;
    private String activeType;

    public StorageManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        Config config = new Config(new File(plugin.getDataFolder(), "config.yml"), Config.YAML);
        String configuredType = config.getString("storage.type", "sqlite");
        String storageType = configuredType == null ? "sqlite" : configuredType.trim().toLowerCase(Locale.ROOT);

        if ("mysql".equals(storageType) || "mariadb".equals(storageType)) {
            mysql = new MySQL(config);
            try {
                mysql.connect();
                connection = mysql.getConnection();
                activeType = "mariadb";
                plugin.getLogger().info("MariaDB storage initialized.");
            } catch (SQLException e) {
                throw new IllegalStateException("Could not initialize Siedler MariaDB storage", e);
            }
        } else {
            sqlite = new SQLite(plugin);
            sqlite.initialize();
            connection = sqlite.getConnection();
            activeType = "sqlite";
        }

        try {
            new InitDB().runInternalScript(plugin, "storage/init.sql");
            plugin.getLogger().info("Internal database init script executed.");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute internal init.sql", e);
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Storage is not initialized");
        }
        return connection;
    }

    public void close() {
        if (connection == null) { return; }
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
            activeType = null;
        }
    }

}
