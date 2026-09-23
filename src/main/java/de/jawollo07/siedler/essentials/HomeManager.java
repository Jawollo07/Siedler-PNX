package de.jawollo07.siedler.essentials;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeManager {

    private static final int MAX_HOME_NAME_LENGTH = 32;

    private final SiedlerPlugin plugin;
    private final StorageManager storage;

    public HomeManager() {
        this.plugin = SiedlerPlugin.getInstance();
        this.storage = plugin.getStorage();
    }

    private Home mapHome(ResultSet resultSet) throws SQLException {
        return new Home(
                resultSet.getString("id"),
                resultSet.getString("player_id"),
                resultSet.getString("name"),
                resultSet.getString("world"),
                resultSet.getDouble("x"),
                resultSet.getDouble("y"),
                resultSet.getDouble("z"),
                resultSet.getDouble("yaw"),
                resultSet.getDouble("pitch"),
                resultSet.getLong("created_at")
        );
    }

    /**
     * Erstellt ein Home an der aktuellen Position des Spielers.
     */
    public Home createHome(Player player, String name) throws SQLException {
        validatePlayer(player);

        String homeName = normalizeName(name);

        String playerId = player.getUniqueId().toString();

        if (getHome(playerId, homeName) != null) {
            throw new IllegalArgumentException(
                    "Du hast bereits ein Home mit diesem Namen."
            );
        }

        if (player.getLevel() == null) {
            throw new IllegalStateException(
                    "Die aktuelle Welt konnte nicht ermittelt werden."
            );
        }

        String id = UUID.randomUUID().toString();

        String world = player.getLevel().getName();

        double x = player.getPosition().getX();
        double y = player.getPosition().getY();
        double z = player.getPosition().getZ();

        double yaw = player.getYaw();
        double pitch = player.getPitch();

        long createdAt = System.currentTimeMillis();

        String sql = """
                INSERT INTO homes (
                    id,
                    player_id,
                    name,
                    world,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, playerId);
            statement.setString(3, homeName);
            statement.setString(4, world);
            statement.setDouble(5, x);
            statement.setDouble(6, y);
            statement.setDouble(7, z);
            statement.setDouble(8, yaw);
            statement.setDouble(9, pitch);
            statement.setLong(10, createdAt);

            statement.executeUpdate();
        }

        return new Home(
                id,
                playerId,
                homeName,
                world,
                x,
                y,
                z,
                yaw,
                pitch,
                createdAt
        );
    }

    /**
     * Holt ein Home anhand der ID.
     */
    public Home getHomeById(String id) throws SQLException {
        if (id == null || id.isBlank()) {
            return null;
        }

        String sql = """
                SELECT
                    id,
                    player_id,
                    name,
                    world,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    created_at
                FROM homes
                WHERE id = ?
                LIMIT 1
                """;

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return mapHome(resultSet);
            }
        }
    }

    /**
     * Holt ein Home anhand von Spieler und Namen.
     */
    public Home getHome(String playerId, String name) throws SQLException {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }

        String homeName = normalizeName(name);

        String sql = """
                SELECT
                    id,
                    player_id,
                    name,
                    world,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    created_at
                FROM homes
                WHERE player_id = ?
                  AND LOWER(name) = LOWER(?)
                LIMIT 1
                """;

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, playerId);
            statement.setString(2, homeName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                return mapHome(resultSet);
            }
        }
    }

    /**
     * Holt ein Home anhand von Spieler und Namen.
     */
    public Home getHome(Player player, String name) throws SQLException {
        validatePlayer(player);

        return getHome(
                player.getUniqueId().toString(),
                name
        );
    }

    /**
     * Gibt alle Homes eines Spielers zurück.
     */
    public List<Home> getHomes(Player player) throws SQLException {
        validatePlayer(player);

        return getHomes(player.getUniqueId().toString());
    }

    /**
     * Gibt alle Homes eines Spielers anhand seiner UUID zurück.
     */
    public List<Home> getHomes(String playerId) throws SQLException {
        if (playerId == null || playerId.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT
                    id,
                    player_id,
                    name,
                    world,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    created_at
                FROM homes
                WHERE player_id = ?
                ORDER BY name COLLATE NOCASE ASC
                """;

        List<Home> homes = new ArrayList<>();

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, playerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    homes.add(mapHome(resultSet));
                }
            }
        }

        return homes;
    }

    /**
     * Löscht ein Home anhand seiner ID.
     */
    public boolean deleteHome(Player player, String name) throws SQLException {
        validatePlayer(player);

        String homeName = normalizeName(name);
        String playerId = player.getUniqueId().toString();

        String sql = """
                DELETE FROM homes
                WHERE player_id = ?
                  AND LOWER(name) = LOWER(?)
                """;

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, playerId);
            statement.setString(2, homeName);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Löscht ein Home anhand seiner ID.
     */
    public boolean deleteHome(String playerId, String homeId)
            throws SQLException {

        if (playerId == null || playerId.isBlank()) {
            return false;
        }

        if (homeId == null || homeId.isBlank()) {
            return false;
        }

        String sql = """
                DELETE FROM homes
                WHERE id = ?
                  AND player_id = ?
                """;

        try (PreparedStatement statement =
                     storage.getConnection().prepareStatement(sql)) {

            statement.setString(1, homeId);
            statement.setString(2, playerId);

            return statement.executeUpdate() > 0;
        }
    }

    private void validatePlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException(
                    "Player darf nicht null sein."
            );
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Der Home-Name darf nicht null sein."
            );
        }

        String normalized = name.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Der Home-Name darf nicht leer sein."
            );
        }

        if (normalized.length() > MAX_HOME_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Der Home-Name darf maximal "
                            + MAX_HOME_NAME_LENGTH
                            + " Zeichen lang sein."
            );
        }

        return normalized;
    }
}