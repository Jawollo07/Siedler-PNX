package de.jawollo07.siedler.storage;

import de.jawollo07.siedler.SiedlerPlugin;
import java.sql.Connection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class InitDB {
    public void initDatabase() {
        SiedlerPlugin plugin = SiedlerPlugin.getInstance();
        try {
            runInternalScript(plugin, "storage/init.sql");
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
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
