package de.jawollo07.siedler.essentials;

public class Home {

    private final String id;
    private final String playerId;
    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double yaw;
    private final double pitch;
    private final long createdAt;

    public Home(
            String id,
            String playerId,
            String name,
            String world,
            double x,
            double y,
            double z,
            double yaw,
            double pitch,
            long createdAt
    ) {
        this.id = id;
        this.playerId = playerId;
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}