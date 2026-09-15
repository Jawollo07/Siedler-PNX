package de.jawollo07.siedler.team;

public record Team(
        String id,
        String name,
        String color,
        int taxBonus,
        int eliminated,
        String eliminationBlock,
        long createdAt,
        int balance
) {
}