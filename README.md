# Siedler 2.0 – PowerNukkitX

Native rewrite of the Siedler Minecraft Bedrock server system for **PowerNukkitX**.

Repository: https://github.com/Jawollo07/Siedler-PNX

## Status

🚧 **Foundation initialized – migration in progress.**

The previous Siedler 1.x implementation is a Bedrock Script API behavior pack. Siedler 2.0 is being rebuilt as a native Java/PowerNukkitX plugin instead of performing a direct JavaScript-to-Java translation.

### Initial foundation

- Java 21 / **Maven** project
- PowerNukkitX plugin descriptor
- central `SiedlerPlugin` lifecycle
- central `SiedlerManager`
- SQLite persistence foundation
- initial `/siedler` command
- configurable gameplay defaults
- architecture prepared for independent gameplay managers

## Target architecture

```text
src/main/java/de/mcjj/siedler/
├── SiedlerPlugin.java
├── core/
├── storage/
├── command/
├── teams/
├── claims/
├── economy/
├── monsters/
├── soldiers/
├── market/
├── essentials/
└── mines/
```

The systems from Siedler 1.x will be migrated in dependency order:

1. Core + persistence
2. Teams + player identity + diplomacy
3. Claims + protection
4. Economy + taxes + TaxBonus
5. Token monsters + outposts + raids
6. Soldiers + AI + groups + levels
7. Market + traders
8. Essentials + inventories + statistics
9. Mines + control system
10. Migration, compatibility and release hardening

## PowerNukkitX target

The project targets **PowerNukkitX 3.0.4** / API **3.0.4**, Java **21**, and Minecraft Bedrock **1.26.45**. PowerNukkitX is supplied by Maven as a `provided` dependency because the server provides it at runtime.

If exact server-version compatibility changes, the dependency and compatibility notes will be updated together with the code.

## Persistence

SQLite is the planned default storage backend. Persistent data is designed around stable player UUID/ID values rather than player names wherever the server API provides a stable identifier.

## Development principles

- Native PowerNukkitX APIs first.
- Managers/services with clear responsibilities instead of one large main class.
- Persistent state must survive restarts.
- Gameplay rules should be configurable.
- Existing Siedler 1.x behavior is the functional reference, while Siedler 2.0 may improve implementation details where PNX provides better native facilities.
- README and `plan.md` are kept synchronized with the implementation.

## Build

Requires **JDK 21** and **Maven**.

```bash
mvn clean package
```

The plugin JAR is produced under `target/`, with the main artifact named `Siedler-2.0.jar`.

## Migration reference

The feature set being migrated includes teams, diplomacy, claims, taxes, TaxBonus, token monsters, outpost capture, monster raids, soldiers (infantry/archer/cavalry), soldier AI, market/traders, homes/TPA, persistent ender/team chests, player statistics, anti-AFK and the minefield/control system.
