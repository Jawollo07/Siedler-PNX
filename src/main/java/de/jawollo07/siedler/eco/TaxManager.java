package de.jawollo07.siedler.eco;

import de.jawollo07.siedler.SiedlerPlugin;
import de.jawollo07.siedler.claim.Claim;
import de.jawollo07.siedler.claim.ClaimManager;
import de.jawollo07.siedler.core.MessageManager;
import de.jawollo07.siedler.team.Team;
import de.jawollo07.siedler.team.TeamManager;
import org.powernukkitx.Player;
import org.powernukkitx.entity.Entity;
import org.powernukkitx.entity.passive.EntityVillager;
import org.powernukkitx.entity.passive.EntityVillagerV2;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.scheduler.TaskHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TaxManager {
    private final SiedlerPlugin plugin;
    private final TeamManager teamManager;
    private final ClaimManager claimManager;
    private final EcoManager ecoManager;
    private final MessageManager messageManager;
    private TaskHandler task;
    private long lastRun;

    public TaxManager(SiedlerPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new TeamManager(plugin);
        this.claimManager = new ClaimManager(plugin);
        this.ecoManager = new EcoManager(plugin);
        this.messageManager = new MessageManager();
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("taxes.enabled", true)) {
            plugin.getLogger().info("Tax system disabled by configuration.");
            return;
        }

        long hours = Math.max(1, plugin.getConfig().getInt("taxes.interval-hours", 24));
        long period = Math.min(Integer.MAX_VALUE, hours * 60L * 60L * 20L);
        task = plugin.getServer().getScheduler().scheduleRepeatingTask(
                plugin, this::collectDueTaxes, (int) Math.max(20L, period));
        plugin.getLogger().info("Tax system started: interval=" + hours + "h");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public synchronized void collectDueTaxes() {
        if (!plugin.getConfig().getBoolean("taxes.enabled", true)) return;

        long now = System.currentTimeMillis();
        if (lastRun != 0 && now - lastRun < 60_000L) return;
        lastRun = now;

        long interval = Math.max(1L,
                plugin.getConfig().getInt("taxes.interval-hours", 24)) * 60L * 60L * 1000L;

        try {
            for (Team team : teamManager.getTeams()) {
                if (team.eliminated() != 0 || !isTeamMemberOnline(team.id())) continue;
                if (isDue(team.id(), now, interval)) collectTax(team);
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Tax cycle failed: " + exception.getMessage());
        }
    }

    /**
     * Calculates and pays the team's villager tax.
     *
     * Formula:
     * villagers × TaxBonus × configured rate
     *
     * Example: 1 villager × TaxBonus 3 × rate 1 = 3 Coins.
     *
     * The team receives the money only when at least one team member is online.
     */
    public TaxResult collectTax(Team team) {
        if (team == null) throw new IllegalArgumentException("team must not be null");

        try {
            if (!isTeamMemberOnline(team.id())) {
                return recordFailure(team.id(), 0, Math.max(1, team.taxBonus()),
                        0, "no_online_member");
            }

            int villagers = countVillagers(team);
            int bonus = Math.max(1, team.taxBonus());
            long rate = Math.max(1L,
                    plugin.getConfig().getInt("taxes.emeralds-per-villager-per-bonus", 1));
            long amount = Math.multiplyExact(Math.multiplyExact((long) villagers, bonus), rate);

            if (amount == 0) {
                recordTaxTransaction(team.id(), villagers, bonus, 0, true, "no_villagers");
                return new TaxResult(team.id(), villagers, bonus, 0, true, "no_villagers");
            }

            if (amount > Integer.MAX_VALUE) {
                return recordFailure(team.id(), villagers, bonus, amount, "amount_too_large");
            }

            // Tax is income for the team: villagers × TaxBonus = Coins.
            ecoManager.addMoney(team.id(), (int) amount, "TAX",
                    "Tagessteuer: " + villagers + " Dorfbewohner × TaxBonus " + bonus);

            recordTaxTransaction(team.id(), villagers, bonus, amount, true, "paid");
            teamManager.notifyAllTeamMembers(team.id(),
                    format(messageManager.getMessage("messages.eco.tax-income"),
                            "amount", String.valueOf(amount),
                            "symbol", ecoManager.getCurrency("s"),
                            "villagers", String.valueOf(villagers),
                            "bonus", String.valueOf(bonus)));
            return new TaxResult(team.id(), villagers, bonus, amount, true, "paid");
        } catch (Exception exception) {
            String reason = exception.getMessage() == null ? "payment_failed" : exception.getMessage();
            int villagers = safeVillagerCount(team);
            int bonus = Math.max(1, team.taxBonus());
            return recordFailure(team.id(), villagers, bonus,
                    calculateAmount(villagers, bonus), reason);
        }
    }

    public int countVillagers(Team team) throws SQLException {
        int count = 0;
        Set<String> countedEntities = new HashSet<>();

        for (Claim claim : claimManager.getClaimsForTeam(team.id())) {
            Level level = plugin.getServer().getLevelByName(claim.world());
            if (level == null) continue;

            for (int x = claim.min_x(); x <= claim.max_x(); x++) {
                for (int z = claim.min_z(); z <= claim.max_z(); z++) {
                    IChunk chunk = level.getProvider().getLoadedChunk(x, z);
                    boolean wasLoaded = chunk != null;

                    if (!wasLoaded && !level.getProvider().loadChunk(x, z, false)) continue;
                    if (!wasLoaded) chunk = level.getProvider().getLoadedChunk(x, z);

                    if (chunk != null) {
                        for (Entity entity : chunk.getEntities().values()) {
                            if (entity instanceof EntityVillagerV2
                                    || entity instanceof EntityVillager
                                    || "minecraft:villager".equals(entity.getIdentifier())) {
                                String id = claim.world() + ":" + entity.getId();
                                if (countedEntities.add(id)) count++;
                            }
                        }
                    }

                    if (!wasLoaded && chunk != null) chunk.unload(true, true);
                }
            }
        }
        return count;
    }

    public boolean isDue(String teamId, long now, long intervalMillis) throws SQLException {
        String sql = "SELECT created_at, successful FROM tax_transactions "
                + "WHERE team_id = ? ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return true;

                long last = resultSet.getLong("created_at");
                if (resultSet.getInt("successful") != 0) return now - last >= intervalMillis;

                long retry = Math.max(1L,
                        plugin.getConfig().getInt("taxes.retry-minutes", 60)) * 60L * 1000L;
                return now - last >= retry;
            }
        }
    }

    private boolean isTeamMemberOnline(String teamId) throws SQLException {
        String sql = "SELECT id FROM players WHERE team_id = ?";
        Set<String> members = new HashSet<>();

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) members.add(resultSet.getString("id"));
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (members.contains(player.getUniqueId().toString())) return true;
        }
        return false;
    }

    private TaxResult recordFailure(String teamId, int villagers, int bonus, long amount, String reason) {
        try {
            recordTaxTransaction(teamId, villagers, bonus, amount, false, reason);
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not record failed tax for " + teamId + ": "
                    + exception.getMessage());
        }
        return new TaxResult(teamId, villagers, bonus, amount, false, reason);
    }

    private void recordTaxTransaction(String teamId, int villagers, int bonus,
                                      long amount, boolean successful, String reason) throws SQLException {
        String sql = "INSERT INTO tax_transactions "
                + "(id, team_id, villager_count, tax_bonus, emeralds_charged, successful, reason, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, teamId);
            statement.setInt(3, Math.max(0, villagers));
            statement.setInt(4, Math.max(1, bonus));
            statement.setLong(5, Math.max(0, amount));
            statement.setInt(6, successful ? 1 : 0);
            statement.setString(7, reason);
            statement.setLong(8, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private String format(String template, String... replacements) {
        String result = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }

    private int safeVillagerCount(Team team) {
        try {
            return countVillagers(team);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long calculateAmount(int villagers, int bonus) {
        long rate = Math.max(1L,
                plugin.getConfig().getInt("taxes.emeralds-per-villager-per-bonus", 1));
        return Math.max(0L, (long) villagers * Math.max(1, bonus) * rate);
    }


    public TaxStatistics getStatistics(String teamId) throws SQLException {
        if (teamId == null || teamId.isBlank()) {
            throw new IllegalArgumentException("Team ID must not be blank");
        }

        String sql = "SELECT COUNT(*) AS total_cycles, "
                + "COALESCE(SUM(CASE WHEN successful = 1 THEN 1 ELSE 0 END), 0) AS successful_cycles, "
                + "COALESCE(SUM(CASE WHEN successful = 0 THEN 1 ELSE 0 END), 0) AS failed_cycles, "
                + "COALESCE(SUM(CASE WHEN successful = 1 AND reason = 'paid' THEN emeralds_charged ELSE 0 END), 0) AS total_coins, "
                + "COALESCE(SUM(CASE WHEN successful = 1 AND reason = 'paid' THEN villager_count ELSE 0 END), 0) AS total_villagers, "
                + "MAX(created_at) AS last_tax_at "
                + "FROM tax_transactions WHERE team_id = ?";

        try (PreparedStatement statement = plugin.getStorage().getConnection().prepareStatement(sql)) {
            statement.setString(1, teamId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new TaxStatistics(teamId, 0, 0, 0, 0L, 0L,
                            null, 0, 0, 0L, null);
                }

                long lastTaxAt = resultSet.getLong("last_tax_at");
                Long lastTaxTimestamp = resultSet.wasNull() ? null : lastTaxAt;
                int lastVillagers = 0;
                int lastBonus = 0;
                long lastAmount = 0L;
                String lastReason = null;

                String latestSql = "SELECT villager_count, tax_bonus, emeralds_charged, reason "
                        + "FROM tax_transactions WHERE team_id = ? "
                        + "ORDER BY created_at DESC LIMIT 1";
                try (PreparedStatement latest = plugin.getStorage().getConnection().prepareStatement(latestSql)) {
                    latest.setString(1, teamId);
                    try (ResultSet latestResult = latest.executeQuery()) {
                        if (latestResult.next()) {
                            lastVillagers = latestResult.getInt("villager_count");
                            lastBonus = latestResult.getInt("tax_bonus");
                            lastAmount = latestResult.getLong("emeralds_charged");
                            lastReason = latestResult.getString("reason");
                        }
                    }
                }

                return new TaxStatistics(
                        teamId,
                        resultSet.getLong("total_cycles"),
                        resultSet.getLong("successful_cycles"),
                        resultSet.getLong("failed_cycles"),
                        resultSet.getLong("total_coins"),
                        resultSet.getLong("total_villagers"),
                        lastTaxTimestamp,
                        lastVillagers,
                        lastBonus,
                        lastAmount,
                        lastReason
                );
            }
        }
    }

    public record TaxStatistics(
            String teamId,
            long totalCycles,
            long successfulCycles,
            long failedCycles,
            long totalCoins,
            long totalVillagers,
            Long lastTaxTimestamp,
            int lastVillagers,
            int lastTaxBonus,
            long lastAmount,
            String lastReason
    ) {}

    public record TaxResult(String teamId, int villagers, int taxBonus,
                            long amount, boolean successful, String reason) {}
}
