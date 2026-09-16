package de.jawollo07.siedler.team;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.storage.StorageManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
 
public class Relations {

    public static final String RELATION_FRIENDLY = "friendly";
    public static final String RELATION_ALLIED = "allied";
    public static final String RELATION_NEUTRAL = "neutral";
    public static final String RELATION_HOSTILE = "hostile";
    public static final String RELATION_ENEMY = "enemy";

    private final StorageManager storage;
    private final TeamManager teamManager;
    private final SiedlerPlugin plugin;

    public Relations(SiedlerPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.storage = this.plugin.getStorage();
        this.teamManager = new TeamManager(this.plugin);
    }

    public static String normalizeRelation(String relation) {
        if (relation == null) {
            return RELATION_NEUTRAL;
        }

        String normalized = relation.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case RELATION_FRIENDLY, RELATION_ALLIED -> RELATION_ALLIED;
            case RELATION_NEUTRAL -> RELATION_NEUTRAL;
            case RELATION_HOSTILE, RELATION_ENEMY -> RELATION_ENEMY;
            default -> RELATION_NEUTRAL;
        };
    }

    public static boolean isValidRelation(String relation) {
        String normalized = normalizeRelation(relation);
        return RELATION_ALLIED.equals(normalized)
                || RELATION_NEUTRAL.equals(normalized)
                || RELATION_ENEMY.equals(normalized);
    }

    public static String toFriendlyAlias(String relation) {
        String normalized = normalizeRelation(relation);
        if (RELATION_ALLIED.equals(normalized)) {
            return RELATION_FRIENDLY;
        }
        if (RELATION_ENEMY.equals(normalized)) {
            return RELATION_HOSTILE;
        }
        return RELATION_NEUTRAL;
    }

    public static String toDatabaseRelation(String relation) {
        return normalizeRelation(relation);
    }

    public String getTeamRelation(String teamNameA, String teamNameB) throws SQLException {
        if (teamNameA == null || teamNameB == null || teamNameA.isBlank() || teamNameB.isBlank()) {
            throw new IllegalArgumentException("Team names must not be blank");
        }

        Team teamA = teamManager.getTeamByName(teamNameA.trim());
        Team teamB = teamManager.getTeamByName(teamNameB.trim());
        if (teamA.id().equals(teamB.id())) {
            return RELATION_ALLIED;
        }

        String sql = "SELECT relation FROM team_diplomacy WHERE team_id = ? AND other_team_id = ?";
        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, teamA.id());
            statement.setString(2, teamB.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return RELATION_NEUTRAL;
                }

                String relation = toFriendlyAlias(resultSet.getString("relation"));
                return relation;
            }
        }
    }

    public boolean setTeamRelation(String teamNameA, String teamNameB, String relation) throws SQLException {
        String normalized = normalizeRelation(relation);
        if (!isValidRelation(normalized)) {
            plugin.getLogger().warning("[Relations] Invalid relation requested: " + relation);
            return false;
        }

        if (teamNameA == null || teamNameB == null || teamNameA.isBlank() || teamNameB.isBlank()) {
            throw new IllegalArgumentException("Team names must not be blank");
        }

        Team teamA = teamManager.getTeamByName(teamNameA.trim());
        Team teamB = teamManager.getTeamByName(teamNameB.trim());
        if (teamA.id().equals(teamB.id())) {
            plugin.getLogger().warning("[Relations] Refused to set relation for the same team: " + teamA.name());
            return false;
        }

        String selectSql = "SELECT relation FROM team_diplomacy WHERE team_id = ? AND other_team_id = ?";
        try (PreparedStatement selectStatement = storage.getConnection().prepareStatement(selectSql)) {
            selectStatement.setString(1, teamA.id());
            selectStatement.setString(2, teamB.id());

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                if (resultSet.next()) {
                    String currentRelation = resultSet.getString("relation");
                    if (normalized.equalsIgnoreCase(currentRelation)) {
                        writeRelationActionLog(teamA.id(), teamB.id(), normalized, "unchanged");
                        return true;
                    }
                }
            }
        }

        String existingSql = "SELECT 1 FROM team_diplomacy WHERE team_id = ? AND other_team_id = ?";
        try (PreparedStatement existingStatement = storage.getConnection().prepareStatement(existingSql)) {
            existingStatement.setString(1, teamA.id());
            existingStatement.setString(2, teamB.id());

            try (ResultSet resultSet = existingStatement.executeQuery()) {
                if (resultSet.next()) {
                    String updateSql = "UPDATE team_diplomacy SET relation = ? WHERE team_id = ? AND other_team_id = ?";
                    try (PreparedStatement updateStatement = storage.getConnection().prepareStatement(updateSql)) {
                        updateStatement.setString(1, normalized);
                        updateStatement.setString(2, teamA.id());
                        updateStatement.setString(3, teamB.id());
                        updateStatement.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO team_diplomacy (team_id, other_team_id, relation) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStatement = storage.getConnection().prepareStatement(insertSql)) {
                        insertStatement.setString(1, teamA.id());
                        insertStatement.setString(2, teamB.id());
                        insertStatement.setString(3, normalized);
                        insertStatement.executeUpdate();
                    }
                }
            }
        }

        String reverseSql = "SELECT 1 FROM team_diplomacy WHERE team_id = ? AND other_team_id = ?";
        try (PreparedStatement reverseStatement = storage.getConnection().prepareStatement(reverseSql)) {
            reverseStatement.setString(1, teamB.id());
            reverseStatement.setString(2, teamA.id());

            try (ResultSet resultSet = reverseStatement.executeQuery()) {
                if (resultSet.next()) {
                    String updateReverseSql = "UPDATE team_diplomacy SET relation = ? WHERE team_id = ? AND other_team_id = ?";
                    try (PreparedStatement updateReverse = storage.getConnection().prepareStatement(updateReverseSql)) {
                        updateReverse.setString(1, normalized);
                        updateReverse.setString(2, teamB.id());
                        updateReverse.setString(3, teamA.id());
                        updateReverse.executeUpdate();
                    }
                } else {
                    String insertReverseSql = "INSERT INTO team_diplomacy (team_id, other_team_id, relation) VALUES (?, ?, ?)";
                    try (PreparedStatement insertReverse = storage.getConnection().prepareStatement(insertReverseSql)) {
                        insertReverse.setString(1, teamB.id());
                        insertReverse.setString(2, teamA.id());
                        insertReverse.setString(3, normalized);
                        insertReverse.executeUpdate();
                    }
                }
            }
        }

        writeRelationActionLog(teamA.id(), teamB.id(), normalized, "set_relation");
        return true;
    }

    public Map<String, String> getRelationsForTeam(String teamName) throws SQLException {
        if (teamName == null || teamName.isBlank()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }

        Team team = teamManager.getTeamByName(teamName.trim());
        String sql = "SELECT other_team_id, relation FROM team_diplomacy WHERE team_id = ? ORDER BY other_team_id";

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, team.id());

            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, String> relations = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String otherTeamId = resultSet.getString("other_team_id");
                    String relation = toFriendlyAlias(resultSet.getString("relation"));
                    relations.put(otherTeamId, relation);
                }
                return Collections.unmodifiableMap(relations);
            }
        }
    }

    public Map<String, String> getRelationsForTeamByName(String teamName) throws SQLException {
        Map<String, String> relations = getRelationsForTeam(teamName);
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : relations.entrySet()) {
            Team otherTeam = teamManager.getTeams().stream()
                    .filter(team -> team.id().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);

            if (otherTeam != null) {
                result.put(otherTeam.name(), entry.getValue());
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private void writeRelationActionLog(String teamIdA, String teamIdB, String relation, String action) throws SQLException {
        String sql = "INSERT INTO team_relation_logs (id, team_id, other_team_id, relation, action, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        String id = java.util.UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        try (PreparedStatement statement = storage.getConnection().prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, teamIdA);
            statement.setString(3, teamIdB);
            statement.setString(4, normalizeRelation(relation));
            statement.setString(5, action == null ? "set_relation" : action);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
    }
}
