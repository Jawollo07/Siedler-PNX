package de.jawollo07.siedler.storage;

import de.jawollo07.siedler.SiedlerPlugin;
import java.sql.Connection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class InitDB {
    private static final int CURRENT_SCHEMA_VERSION = 4;

    public void initDatabase() {
        SiedlerPlugin plugin = SiedlerPlugin.getInstance();
        try {
            runInternalScript(plugin, "storage/init.sql");
            upgradeDatabase(plugin.getStorage().getConnection());
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void upgradeDatabase(Connection connection) throws Exception {
        int version = getSchemaVersion(connection);

        if (version < CURRENT_SCHEMA_VERSION) {
            if (version < 2) {
                migrateV1ToV2(connection);
                setSchemaVersion(connection, 2);
            }
            if (version < 3) {
                migrateV2ToV3(connection);
                setSchemaVersion(connection, 3);
            }
            if (version < 4) {
                migrateV3ToV4(connection);
                setSchemaVersion(connection, 4);
            }
        }
    }

    private int getSchemaVersion(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT version FROM schema_version ORDER BY version DESC LIMIT 1")) {
                if (!resultSet.next()) {
                    return 0;
                }
                return resultSet.getInt("version");
            }
        }
    }

    private void setSchemaVersion(Connection connection, int version) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE schema_version SET version = " + version);
        }
    }

    private void migrateV1ToV2(Connection connection) throws Exception {
        ensureColumnExists(connection, "teams", "balance", "INTEGER NOT NULL DEFAULT 0");
        ensureColumnExists(connection, "players", "team_id", "TEXT");
        ensureColumnExists(connection, "teams", "created_at", "BIGINT NOT NULL DEFAULT 0");

        String dbProduct = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
            updateColumnType(connection, "teams", "created_at", "BIGINT NOT NULL DEFAULT 0");
            updateColumnType(connection, "players", "first_join", "BIGINT NOT NULL DEFAULT 0");
            updateColumnType(connection, "players", "last_seen", "BIGINT NOT NULL DEFAULT 0");
        }
    }

    private void migrateV2ToV3(Connection connection) throws Exception {
        ensureColumnExists(connection, "chat_messages", "player_name", "TEXT NOT NULL DEFAULT ''");
        ensureColumnExists(connection, "chat_messages", "world", "TEXT NOT NULL DEFAULT ''");
        ensureColumnExists(connection, "chat_messages", "x", "REAL NOT NULL DEFAULT 0");
        ensureColumnExists(connection, "chat_messages", "y", "REAL NOT NULL DEFAULT 0");
        ensureColumnExists(connection, "chat_messages", "z", "REAL NOT NULL DEFAULT 0");
    }

    private void migrateV3ToV4(Connection connection) throws Exception {
        ensureColumnExists(connection, "chat_messages", "target", "TEXT NOT NULL DEFAULT 'global'");
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (columns.next()) {
                return;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private void updateColumnType(Connection connection, String tableName, String columnName, String columnDefinition) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (!columns.next()) {
                return;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnDefinition
            );
        }
    }

    public void runInternalScript(SiedlerPlugin plugin, String resourcePath) throws Exception {
        if (plugin.getStorage() == null || plugin.getStorage().getConnection() == null) {
            throw new IllegalStateException("Storage is not initialized before executing init.sql");
        }

        Connection connection = plugin.getStorage().getConnection();
        ClassLoader classLoader = InitDB.class.getClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Internal SQL resource not found: " + resourcePath);
            }

            StringBuilder sql = new StringBuilder();
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                int c;
                while ((c = reader.read()) != -1) {
                    sql.append((char) c);
                }
            }

            String script = sql.toString().replaceAll("(?m)^\\s*```(?:sql)?\\s*$", "");
            String databaseProduct = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);
            if (databaseProduct.contains("mariadb") || databaseProduct.contains("mysql")) {
                script = script.replaceAll(
                    "(?im)^(\\s*(?:id|name|team_id|other_team_id|source_type|source_id|"
                        + "world|owner_team_id|owner_player_id|owner_id|round_id|entity_uuid|"
                        + "group_id|player_id|key|inventory_type)\\s+)TEXT\\b",
                    "$1VARCHAR(255)"
                );
                script = script.replaceAll("(?is)PRAGMA\\s+foreign_keys\\s*=\\s*ON\\s*;", "");
                script = script.replaceAll("(?im)\\bBEGIN\\s+TRANSACTION\\b", "BEGIN");
                script = script.replaceAll(
                    "(?im)^(\\s*)key(\\s+VARCHAR\\(255\\)\\s+PRIMARY KEY)",
                    "$1`key`$2"
                );
            }

            try (Statement stmt = connection.createStatement()) {
                String[] statements = script.split(";");
                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (!trimmedStatement.isEmpty()) {
                        stmt.executeUpdate(trimmedStatement);
                    }
                }
            }
        }
    }
}
