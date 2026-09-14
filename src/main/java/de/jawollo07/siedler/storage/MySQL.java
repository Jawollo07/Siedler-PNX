package de.jawollo07.siedler.storage;

import de.jawollo07.siedler.core.ConfigManager;
import org.powernukkitx.utils.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySQL {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public MySQL() {
        this((Config) null);
    }

    public MySQL(ConfigManager configManager) {
        this(configManager != null ? configManager.getConfig() : null);
    }

    public MySQL(Config config) {
        this.host = config != null ? config.getString("mysql.host", "localhost") : "localhost";
        this.port = config != null ? config.getInt("mysql.port", 3306) : 3306;
        this.database = config != null ? config.getString("mysql.database", "siedler") : "siedler";
        this.username = config != null ? config.getString("mysql.username", "root") : "root";
        this.password = config != null ? config.getString("mysql.password", "") : "";
    }

    public void connect() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MariaDB JDBC driver not found", e);
        }

        String url = "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        connection = DriverManager.getConnection(url, username, password);
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("MySQL connection is not initialized.");
        }
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
