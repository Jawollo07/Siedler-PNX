package de.jawollo07.siedler.eco;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;
import org.powernukkitx.utils.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EcoManager {
    private final StorageManager storageManager;
    private final Map<String, Integer> teamMoney = new ConcurrentHashMap<>();
    private final Config config;

    public EcoManager(SiedlerPlugin plugin) {
        this.storageManager = plugin.getStorage();
        this.config = plugin.getConfig();
    }

    public String getCurrency(String nameOrSymbol) {
        if ("n".equals(nameOrSymbol)) {
            return config.getString("economy.name");
        }
        if ("s".equals(nameOrSymbol)) {
            return config.getString("economy.symbol");
        }
        return null;
    }

    public synchronized void teamCreation(String teamID) {
        String normalizedTeamId = requireTeamId(teamID);
        String sql = "INSERT INTO team_money (id, team_id, balance) VALUES (?, ?, 0)";

        try (PreparedStatement statement = storageManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, normalizedTeamId);
            statement.executeUpdate();
            teamMoney.put(normalizedTeamId, 0);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not create economy account for team " + normalizedTeamId, exception);
        }
    }

    public Integer getMoney(String teamID) {
        String normalizedTeamId = requireTeamId(teamID);
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
                    "Could not load economy account for team " + normalizedTeamId, exception);
        }
    }

    public synchronized void setMoney(String teamID, Integer balance) throws SQLException {
        if (balance == null || balance < 0) {
            throw new IllegalArgumentException("balance must be >= 0");
        }
        long delta = balance.longValue() - currentBalance(teamID);
        if (delta != 0) {
            changeMoney(teamID, delta, "SET", "Kontostand gesetzt", null);
        }
    }

    public synchronized void addMoney(String teamID, Integer amount) throws SQLException {
        requirePositiveAmount(amount);
        changeMoney(teamID, amount.longValue(), "ADD", "Geld hinzugefügt", null);
    }

    public synchronized void removeMoney(String teamID, Integer amount) throws SQLException {
        requirePositiveAmount(amount);
        changeMoney(teamID, -amount.longValue(), "REMOVE", "Geld abgezogen", null);
    }

    public synchronized void changeMoney(
            String teamID,
            long delta,
            String transactionType,
            String description,
            String memberId
    ) throws SQLException {
        String normalizedTeamId = requireTeamId(teamID);
        if (delta == 0) {
            return;
        }
        if (transactionType == null || transactionType.isBlank()) {
            throw new IllegalArgumentException("transactionType must not be blank");
        }

        Connection connection = storageManager.getConnection();
        boolean previousAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);

            long oldBalance = currentBalance(connection, normalizedTeamId);
            long newBalance = oldBalance + delta;
            if (newBalance < 0 || newBalance > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Resulting balance is outside the supported range");
            }

            String updateSql = delta < 0
                    ? "UPDATE team_money SET balance = balance - ? WHERE team_id = ? AND balance >= ?"
                    : "UPDATE team_money SET balance = balance + ? WHERE team_id = ?";

            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setLong(1, Math.abs(delta));
                statement.setString(2, normalizedTeamId);
                if (delta < 0) {
                    statement.setLong(3, Math.abs(delta));
                }
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Economy account not found or insufficient funds for team "
                            + normalizedTeamId);
                }
            }

            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO transactions "
                            + "(id, team_id, member_id, amount, transaction_type, description, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, normalizedTeamId);
                if (memberId == null || memberId.isBlank()) {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(3, memberId.trim());
                }
                statement.setLong(4, delta);
                statement.setString(5, transactionType.trim().toUpperCase());
                statement.setString(6, description);
                statement.setLong(7, now);
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO balance_history (id, team_id, date, balance) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, normalizedTeamId);
                statement.setLong(3, now);
                statement.setLong(4, newBalance);
                statement.executeUpdate();
            }

            connection.commit();
            teamMoney.put(normalizedTeamId, (int) newBalance);
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
                // Keep the original exception/result.
            }
        }
    }

    private long currentBalance(String teamID) throws SQLException {
        return currentBalance(storageManager.getConnection(), requireTeamId(teamID));
    }

    private long currentBalance(Connection connection, String teamID) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM team_money WHERE team_id = ? LIMIT 1")) {
            statement.setString(1, teamID);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Economy account not found for team " + teamID);
                }
                return resultSet.getLong("balance");
            }
        }
    }

    private String requireTeamId(String teamID) {
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("teamID must not be blank");
        }
        return teamID.trim();
    }

    private void requirePositiveAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }
}
