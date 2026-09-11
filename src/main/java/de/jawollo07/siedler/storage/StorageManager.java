package de.mcjj.siedler.storage;

import de.mcjj.siedler.SiedlerPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Persistent storage foundation. Gameplay managers will use this service. */
public final class StorageManager {
    private final SiedlerPlugin plugin;
    private Connection connection;

    public StorageManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File database = new File(plugin.getDataFolder(), "siedler.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = ON");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS teams (id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE, tax_bonus INTEGER NOT NULL DEFAULT 1, eliminated INTEGER NOT NULL DEFAULT 0)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS team_members (team_id TEXT NOT NULL, player_id TEXT NOT NULL UNIQUE, PRIMARY KEY(team_id, player_id), FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_stats (player_id TEXT PRIMARY KEY, kills INTEGER NOT NULL DEFAULT 0, deaths INTEGER NOT NULL DEFAULT 0)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize Siedler SQLite storage", e);
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Storage is not initialized");
        }
        return connection;
    }

    public void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not close SQLite storage: " + e.getMessage());
        } finally {
            connection = null;
        }
    }
}
