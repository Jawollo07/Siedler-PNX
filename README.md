# Siedler 2.0 – PowerNukkitX

Native rewrite of the Siedler Minecraft Bedrock server system for **PowerNukkitX**.

Repository: https://github.com/Jawollo07/Siedler-PNX

## Status

🚧 **Phase 6 – Monster, Tokens & Outposts: migration in progress.**

The previous Siedler 1.x implementation is a Bedrock Script API behavior pack. Siedler 2.0 is being rebuilt as a native Java/PowerNukkitX plugin rather than as a direct JavaScript-to-Java translation.

### Current implementation

- Java 21 / **Maven** project
- PowerNukkitX plugin descriptor
- Central `SiedlerPlugin` lifecycle
- Central manager structure
- SQLite persistence foundation
- MariaDB JDBC support
- Initial `/siedler` command
- Configurable gameplay defaults
- TeamManager
- Team creation and deletion
- Team membership with stable player IDs
- Team colors / display data
- Team chat
- Diplomacy
- Team elimination and permanent spectator handling
- Claim management
- Claim block-break and block-place protection
- Economy service with persistent team balances
- Transaction and balance-history logging
- Villager-based team taxes
- TaxBonus-based tax calculation
- Online-team requirement for tax income
- Tax statistics with in-game SimpleForm UI
- Tree Command API for command routing
- Administrative command routes protected by `siedler.admin`
- Essentials Home system with persistent homes
- `/sethome`, `/home`, `/homes` and `/delhome`
- Home teleportation with saved world, position and rotation
- Administrative management GUI with player information and moderation actions
- Persistent moderation history for warnings, kicks, bans and temporary bans
- Configurable Anti-AFK system with warning and automatic kick
- Phase 6 TokenManager with persistent token rounds
- Configurable token spawning with active-token limit and safe ground-position selection
- Token defeat rewards a permanent team TaxBonus
- Persistent personal Enderchest (`/ec`) with 27 slots
- Shared persistent Team-Enderchest (`/tec`) with 54 slots
- Admin read/write access to personal and Team-Enderchests through `/verwaltung`

## Economy & Taxes

The economy uses `team_money` as the central source for team balances. Balance changes are performed transactionally and recorded in the transaction and balance-history tables.

### Tax formula

The current tax formula is:

```text
Coins = Villager × TaxBonus × Rate
```

The default rate is `1`.

Examples:

```text
1 Villager × TaxBonus 1 = 1 Coin
1 Villager × TaxBonus 3 = 3 Coins
10 Villagers × TaxBonus 3 = 30 Coins
```

Tax is **team income**. It is added to the team's balance rather than charged from it.

A team receives its tax income only when **at least one team member is online**. Eliminated teams do not receive tax income.

The tax cycle runs by default every 24 hours. Failed tax cycles are recorded and can be retried using the configured retry interval.

Tax records contain the team, villager count, TaxBonus, calculated amount, success state, reason and timestamp.

`/eco stats` opens an in-game SimpleForm for the player's own team. `/eco stats <Team>` can display a selected team's tax statistics. The UI shows total tax income, successful and failed collections, counted villagers and details of the latest tax collection.

## Command architecture

All migrated command managers use the **PowerNukkitX Tree Command API**. Commands are structured into normal player routes and, where administrative actions are required, an explicit `admin` subcommand.

Administrative routes require the permission:

```text
siedler.admin
```

Current command structure:

| Command | Player routes | Admin routes |
|---|---|---|
| `/claim` | `help`, `info` | `admin help`, `admin set`, `admin delete` |
| `/team` | `help`, `list`, `info <name>` | `admin help`, `admin create`, `admin delete`, `admin add`, `admin remove`, `admin setcolor` |
| `/diplomatie` | `help`, `set`, `show` | `admin help`, `admin set`, `admin show`, `admin list` |
| `/elimination` | `help`, `list` | `admin help`, `admin eliminate`, `admin deeliminate` |
| `/eco` | `help`, `show`, `stats [Team]` | `admin help`, `admin set`, `admin add`, `admin remove` |
| `/sethome` | `<Name>` | – |
| `/home` | `<Name>` | – |
| `/homes` | Home-GUI | – |
| `/delhome` | `<Name>` | – |
| `/tpa` | `<Spieler>` | – |
| `/tpaccept` | – | – |
| `/tpdeny` | – | – |
| `/tpacancel` | – | – |
| `/ec` | own 27-slot Enderchest | – |
| `/tec` | own team's shared 54-slot Enderchest | – |
| `/verwaltung` | – | Enderchest admin management, moderation and player management |

Communication commands such as `/dm` and `/teamchat` remain normal player commands because they are not administrative management commands.

The central `CommandManager` remains responsible for registering commands with the PowerNukkitX command map. Individual command managers are responsible for their own Tree API routes and permissions.

## Versioning

Project versions are coupled to the migration plan using:

```text
PHASE.FEATURE.PATCH
```

- **PHASE** identifies the current migration phase.
- **FEATURE** identifies a functional milestone inside that phase.
- **PATCH** is reserved for bugfixes and small non-feature changes.
- Development versions use the `-SNAPSHOT` suffix.
- Each new migration phase starts at `X.0.0-SNAPSHOT`.
- The first stable complete release is planned as **`10.0.0`**.

Example for the Teams phase:

| Version | Milestone |
|---|---|
| `2.0.0` | TeamManager foundation |
| `2.1.0` | Team creation/deletion |
| `2.2.0` | Team members + stable player IDs |
| `2.3.0` | Team colors/display |
| `2.4.0` | Team chat |
| `2.5.0` | Diplomacy |
| `2.6.0` | Team elimination |
| `2.7.0` | Permanent spectator after elimination |
| `2.8.0` | Configurable elimination block |

The version is therefore a progress indicator for the migration plan, not an independent semantic-versioning roadmap.

## Target architecture

```text
src/main/java/de/jawollo07/siedler/
├── SiedlerPlugin.java
├── core/
├── storage/
├── command/
├── team/
├── claims/
├── economy/
├── monsters/
├── soldiers/
├── market/
├── essentials/
└── mines/
```

The Token system persists token rounds and defeated-token state in `token_rounds` and `tokens`. Admins can use `/token admin start`, `/token admin spawn` and `/token admin status`. Automatic spawning is configurable under `monsters.token`.

The TPA system uses short-lived in-memory requests with a 60-second timeout and explicit accept/deny/cancel commands.

The systems from Siedler 1.x are migrated in dependency order:

1. Core + persistence
2. Teams + player identity + diplomacy
3. Claims + protection
4. Economy + taxes + TaxBonus
5. Token monsters + outposts + raids
6. Market + traders
7. Essentials + inventories + statistics
8. Mines + control system
9. Soldiers + AI + groups + levels
10. Migration, compatibility and release hardening

The detailed checklist and current progress are maintained in [`plan.md`](plan.md).

## PowerNukkitX target

The project targets **PowerNukkitX 3.0.4-SNAPSHOT** / API **3.0.x**, Java **21**, and Minecraft Bedrock **1.26.45**. PowerNukkitX is supplied by Maven as a `provided` dependency because the server provides it at runtime.

If exact server-version compatibility changes, the dependency and compatibility notes will be updated together with the code.

## Persistence

SQLite is the default local storage backend, with MariaDB JDBC support available for database-backed deployments. Persistent data is designed around stable player UUID/ID values rather than player names wherever the server API provides a stable identifier.

## Development principles

- Native PowerNukkitX APIs first.
- Use the PowerNukkitX Tree Command API for command routing instead of manual argument parsing.
- Separate normal player functionality from administrative management through explicit `admin` routes.
- Administrative command routes require `siedler.admin`.
- Managers/services have clear responsibilities instead of one large main class.
- Persistent state must survive restarts.
- Gameplay rules should be configurable.
- Existing Siedler 1.x behavior is the functional reference, while Siedler 2.0 may improve implementation details where PowerNukkitX provides better native facilities.
- README and `plan.md` are kept synchronized with the implementation.
- Version milestones follow `plan.md`.

## Build

Requires **JDK 21** and **Maven**.

```bash
mvn clean package
```

The plugin JAR is produced under `target/`. The current Maven project version is **`3.3.1`**.

## Migration reference

The feature set being migrated includes teams, diplomacy, claims, taxes, TaxBonus, token monsters, outpost capture, monster raids, soldiers (infantry/archer/cavalry), soldier AI, market/traders, homes/TPA, persistent ender/team chests, player statistics, anti-AFK, and the minefield/control system.
