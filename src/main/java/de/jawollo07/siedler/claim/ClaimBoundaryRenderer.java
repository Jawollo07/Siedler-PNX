package de.jawollo07.siedler.claim;

import de.jawollo07.siedler.SiedlerPlugin;
import org.powernukkitx.Player;
import org.powernukkitx.event.Listener;
import org.powernukkitx.level.particle.DustParticle;
import org.powernukkitx.math.Vector3;

import java.sql.SQLException;
import java.util.List;

/**
 * Permanently renders nearby claim boundaries as client-side particles.
 *
 * <p>Particles are sent only to players within the configured radius. This
 * keeps the visualization per-player and avoids creating persistent world
 * entities.</p>
 */
public final class ClaimBoundaryRenderer implements Runnable, Listener {
    private static final int OWN_R = 60;
    private static final int OWN_G = 220;
    private static final int OWN_B = 90;

    private static final int OTHER_R = 240;
    private static final int OTHER_G = 70;
    private static final int OTHER_B = 70;

    private final SiedlerPlugin plugin;
    private final ClaimManager claimManager;

    public ClaimBoundaryRenderer(SiedlerPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin darf nicht null sein");
        }
        this.plugin = plugin;
        this.claimManager = new ClaimManager(plugin);
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("claims.visualization.enabled", true)) {
            return;
        }

        int radius = Math.max(4, plugin.getConfig().getInt("claims.visualization.radius", 32));
        int spacing = Math.max(1, plugin.getConfig().getInt("claims.visualization.spacing", 2));
        int height = plugin.getConfig().getInt("claims.visualization.height", 0);

        final List<Claim> claims;
        try {
            claims = claimManager.getAllClaims();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Claim-Grenzen konnten nicht geladen werden: " + exception.getMessage());
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player == null || player.getLevel() == null || !player.isAlive()) {
                continue;
            }

            String world = player.getLevel().getName();
            double playerX = player.getX();
            double playerZ = player.getZ();
            Claim playerClaim = claimManager.getClaimAtChunk(
                    world, player.getChunkX(), player.getChunkZ()
            );

            for (Claim claim : claims) {
                if (!world.equals(claim.world())) {
                    continue;
                }

                int minBlockX = claim.min_x() * 16;
                int minBlockZ = claim.min_z() * 16;
                int maxBlockX = claim.max_x() * 16 + 15;
                int maxBlockZ = claim.max_z() * 16 + 15;

                if (distanceSquaredToRectangle(playerX, playerZ,
                        minBlockX, minBlockZ, maxBlockX, maxBlockZ) > (double) radius * radius) {
                    continue;
                }

                boolean own = playerClaim != null && playerClaim.id().equals(claim.id());
                int y = height > 0 ? height : Math.max(1, (int) Math.floor(player.getY()) + 1);
                renderBoundary(player, minBlockX, minBlockZ, maxBlockX, maxBlockZ,
                        y, spacing, own);
            }
        }
    }

    private void renderBoundary(
            Player player,
            int minX,
            int minZ,
            int maxX,
            int maxZ,
            int y,
            int spacing,
            boolean own
    ) {
        int r = own ? OWN_R : OTHER_R;
        int g = own ? OWN_G : OTHER_G;
        int b = own ? OWN_B : OTHER_B;

        renderLine(player, minX, minZ, maxX, minZ, y, spacing, r, g, b);
        renderLine(player, minX, maxZ, maxX, maxZ, y, spacing, r, g, b);
        renderLine(player, minX, minZ, minX, maxZ, y, spacing, r, g, b);
        renderLine(player, maxX, minZ, maxX, maxZ, y, spacing, r, g, b);
    }

    private void renderLine(
            Player player,
            int x1,
            int z1,
            int x2,
            int z2,
            int y,
            int spacing,
            int r,
            int g,
            int b
    ) {
        int dx = Integer.compare(x2, x1);
        int dz = Integer.compare(z2, z1);
        int length = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));

        for (int i = 0; i <= length; i += spacing) {
            int x = x1 + dx * i;
            int z = z1 + dz * i;

            player.getLevel().addParticle(
                    new DustParticle(new Vector3(x + 0.5, y + 0.05, z + 0.5), r, g, b),
                    player
            );
        }

        if (length % spacing != 0) {
            player.getLevel().addParticle(
                    new DustParticle(new Vector3(x2 + 0.5, y + 0.05, z2 + 0.5), r, g, b),
                    player
            );
        }
    }

    private double distanceSquaredToRectangle(
            double x,
            double z,
            double minX,
            double minZ,
            double maxX,
            double maxZ
    ) {
        double dx = Math.max(minX - x, Math.max(0.0, x - maxX));
        double dz = Math.max(minZ - z, Math.max(0.0, z - maxZ));
        return dx * dx + dz * dz;
    }
}
