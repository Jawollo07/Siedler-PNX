package de.jawollo07.siedler.claim;

public record Claim (
    String id,
    String teamID,
    String world,
    Integer min_x,
    Integer min_z,
    Integer max_x,
    Integer max_z
) {}
