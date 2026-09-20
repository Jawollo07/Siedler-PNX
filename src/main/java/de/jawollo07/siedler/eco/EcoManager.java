package de.jawollo07.siedler.eco;
import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.powernukkitx.utils.Config;

public class EcoManager {
    private final StorageManager storageManager;
    private final Map<String, Integer> teamMoney = new ConcurrentHashMap<>();
    private final Config config;
    public EcoManager(SiedlerPlugin plugin) {
        this.storageManager = plugin.getStorage();
        this.config = plugin.getConfig();
    }
    public String getCurrency(String NameOrSymbol) {
        if(NameOrSymbol == "n") {
            String ecoName = config.getString("economy.name");
            return ecoName;
        } else if(NameOrSymbol == "s") {
            String symbol = config.getString("economy.symbol");
            return symbol;
        } else {
            return null;
        }
    }
    public synchronized void teamCreation(String teamID) {
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }

        String normalizedTeamId = teamID.trim();
        String selectSql = "SELECT balance FROM team_money WHERE team_id = ? LIMIT 1";
        String insertSql = "INSERT INTO team_money (id, team_id, balance) VALUES (?, ?, 0)";

        try (PreparedStatement select = storageManager.getConnection().prepareStatement(selectSql)) {
            select.setString(1, normalizedTeamId);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    teamMoney.put(normalizedTeamId, resultSet.getInt("balance"));
                    return;
                }
            }

            try (PreparedStatement insert = storageManager.getConnection().prepareStatement(insertSql)) {
                insert.setString(1, normalizedTeamId);
                insert.setString(2, normalizedTeamId);
                insert.executeUpdate();
            }
            teamMoney.put(normalizedTeamId, 0);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not create economy account for team " + normalizedTeamId,
                    exception
            );
        }
    }

    public Integer getMoney(String teamID) {
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }
        String normalizedTeamId = teamID.trim();
        Integer cachedBalance = teamMoney.get(normalizedTeamId);
        if (cachedBalance != null) {
            return cachedBalance;
        }

        String sql = "SELECT balance FROM team_money WHERE team_id = ? LIMIT 1";
        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, normalizedTeamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                int balance = resultSet.getInt("balance");
                teamMoney.put(normalizedTeamId, balance);
                return balance;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not load economy account for team " + normalizedTeamId,
                    exception
            );
        }
    }
    public synchronized void setMoney(String teamID, Integer balance) throws SQLException {
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }

        if (balance == null) {
            throw new IllegalArgumentException("balance must not be null");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("balance must not be negative");
        }

        String normalizedTeamId = teamID.trim();
        String sql = "UPDATE team_money SET balance = ? WHERE team_id = ?";

        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setInt(1, balance);
            statement.setString(2, normalizedTeamId);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Economy account not found for team " + normalizedTeamId);
            }
        }

        teamMoney.put(normalizedTeamId, balance);
    }
    public synchronized void addMoney(String teamID, Integer balance) throws SQLException {
        Integer balance_now = getMoney(teamID);
        Integer new_balance = balance_now + balance;
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }

        String normalizedTeamId = teamID.trim();
        String sql = "UPDATE team_money SET balance = ? WHERE team_id = ?";

        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setInt(1, new_balance);
            statement.setString(2, normalizedTeamId);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Economy account not found for team " + normalizedTeamId);
            }
        }

        teamMoney.put(normalizedTeamId, new_balance);
    }
    public synchronized void removeMoney(String teamID, Integer balance) throws SQLException {
        Integer balance_now = getMoney(teamID);
        Integer new_balance = balance_now - balance;
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }

        String normalizedTeamId = teamID.trim();
        String sql = "UPDATE team_money SET balance = ? WHERE team_id = ?";

        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setInt(1, new_balance);
            statement.setString(2, normalizedTeamId);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Economy account not found for team " + normalizedTeamId);
            }
        }

        teamMoney.put(normalizedTeamId, new_balance);
    }
}
