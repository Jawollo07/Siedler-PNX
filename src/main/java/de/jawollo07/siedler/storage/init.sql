```sql
-- ============================================================
-- Siedler 2.0 - Initial Database Schema
-- Database: SQLite
-- ============================================================

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- ============================================================
-- Schema version
-- ============================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER NOT NULL
);

INSERT INTO schema_version (version)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM schema_version
);
-- ============================================================
-- Teams
-- ============================================================

CREATE TABLE IF NOT EXISTS teams (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    color TEXT NOT NULL DEFAULT 'WHITE',

    tax_bonus INTEGER NOT NULL DEFAULT 1,

    eliminated INTEGER NOT NULL DEFAULT 0,
    elimination_block TEXT,

    created_at BIGINT NOT NULL,

    balance INTEGER NOT NULL DEFAULT 0,
    
    CHECK (tax_bonus >= 1),
    CHECK (eliminated IN (0, 1))
);

-- ============================================================
-- Players
-- ============================================================

CREATE TABLE IF NOT EXISTS players (
    id VARCHAR(36) PRIMARY KEY,
    last_name TEXT NOT NULL,

    team_id VARCHAR(36),

    eliminated INTEGER NOT NULL DEFAULT 0,

    first_join BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE SET NULL,

    CHECK (eliminated IN (0, 1))
);

-- ============================================================
-- Economy
-- ============================================================

CREATE TABLE IF NOT EXISTS team_money (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    balance INTEGER NOT NULL,

    UNIQUE (team_id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    member_id VARCHAR(36),

    amount INTEGER NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    description TEXT,
    created_at BIGINT NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (member_id)
        REFERENCES players(id)
        ON DELETE SET NULL,

    CHECK (amount != 0)
);

CREATE TABLE IF NOT EXISTS balance_history (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    date BIGINT NOT NULL,
    balance INTEGER NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_balance_history_team
    ON balance_history(team_id);

CREATE INDEX IF NOT EXISTS idx_balance_history_date
    ON balance_history(date);

CREATE INDEX IF NOT EXISTS idx_transactions_team
    ON transactions(team_id);

CREATE INDEX IF NOT EXISTS idx_transactions_created
    ON transactions(created_at);

CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    player_name TEXT NOT NULL,
    world VARCHAR(255) NOT NULL,
    target TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    message TEXT NOT NULL,
    created_at BIGINT NOT NULL,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);
-- ============================================================
-- Team Diplomacy
-- ============================================================

CREATE TABLE IF NOT EXISTS team_diplomacy (
    team_id VARCHAR(36) NOT NULL,
    other_team_id VARCHAR(36) NOT NULL,

    relation TEXT NOT NULL DEFAULT 'neutral',

    PRIMARY KEY (team_id, other_team_id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (other_team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (team_id != other_team_id),
    CHECK (relation IN ('allied', 'neutral', 'enemy'))
);

CREATE TABLE IF NOT EXISTS team_relation_logs (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    other_team_id VARCHAR(36) NOT NULL,
    relation TEXT NOT NULL DEFAULT 'neutral',
    action TEXT NOT NULL DEFAULT 'set_relation',
    created_at BIGINT NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (other_team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (relation IN ('allied', 'neutral', 'enemy'))
);

-- ============================================================
-- Tax Bonus Sources
-- ============================================================

CREATE TABLE IF NOT EXISTS team_bonus_sources (
    id VARCHAR(36) PRIMARY KEY,

    team_id VARCHAR(36) NOT NULL,

    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(255) NOT NULL,

    amount INTEGER NOT NULL DEFAULT 1,
    permanent INTEGER NOT NULL DEFAULT 1,

    created_at BIGINT NOT NULL,

    UNIQUE (team_id, source_type, source_id),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (amount > 0),
    CHECK (permanent IN (0, 1))
);

-- ============================================================
-- Claims
-- ============================================================

CREATE TABLE IF NOT EXISTS claims (
    id VARCHAR(36) PRIMARY KEY,

    team_id VARCHAR(36) NOT NULL,

    world VARCHAR(255) NOT NULL,

    min_x INTEGER NOT NULL,
    min_z INTEGER NOT NULL,
    max_x INTEGER NOT NULL,
    max_z INTEGER NOT NULL,

    created_at BIGINT NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (min_x <= max_x),
    CHECK (min_z <= max_z)
);

-- ============================================================
-- Outposts
-- ============================================================

CREATE TABLE IF NOT EXISTS outposts (
    id VARCHAR(36) PRIMARY KEY,

    name VARCHAR(255) NOT NULL UNIQUE,

    world VARCHAR(255) NOT NULL,

    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,

    radius INTEGER NOT NULL DEFAULT 12,

    owner_team_id VARCHAR(36),

    captured_at BIGINT,

    created_at BIGINT NOT NULL,

    FOREIGN KEY (owner_team_id)
        REFERENCES teams(id)
        ON DELETE SET NULL,

    CHECK (radius > 0)
);

-- ============================================================
-- Token Rounds
-- ============================================================

CREATE TABLE IF NOT EXISTS token_rounds (
    id VARCHAR(36) PRIMARY KEY,

    started_at BIGINT NOT NULL,

    completed_at BIGINT,

    completed INTEGER NOT NULL DEFAULT 0,

    CHECK (completed IN (0, 1))
);

-- ============================================================
-- Token Monsters
-- ============================================================

CREATE TABLE IF NOT EXISTS tokens (
    id VARCHAR(36) PRIMARY KEY,

    round_id VARCHAR(36) NOT NULL,

    entity_uuid VARCHAR(36) UNIQUE,

    world VARCHAR(255) NOT NULL,

    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,

    defeated INTEGER NOT NULL DEFAULT 0,

    spawned_at BIGINT NOT NULL,
    defeated_at BIGINT,

    FOREIGN KEY (round_id)
        REFERENCES token_rounds(id)
        ON DELETE CASCADE,

    CHECK (defeated IN (0, 1))
);

-- ============================================================
-- Pillager Raids
-- ============================================================

CREATE TABLE IF NOT EXISTS raids (
    id VARCHAR(36) PRIMARY KEY,
    outpost_id VARCHAR(36) NOT NULL,
    outpost_name VARCHAR(255) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    wave INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    started_at BIGINT NOT NULL,
    finished_at BIGINT,

    FOREIGN KEY (outpost_id)
        REFERENCES outposts(id)
        ON DELETE CASCADE,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (wave >= 0)
);

-- ============================================================
-- Soldier Groups
-- ============================================================

CREATE TABLE IF NOT EXISTS soldier_groups (
    id VARCHAR(36) PRIMARY KEY,

    owner_player_id VARCHAR(36) NOT NULL,

    team_id VARCHAR(36) NOT NULL,

    name VARCHAR(255) NOT NULL,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (owner_player_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    UNIQUE (owner_player_id, name)
);

-- ============================================================
-- Soldiers
-- ============================================================

CREATE TABLE IF NOT EXISTS soldiers (
    id VARCHAR(36) PRIMARY KEY,

    entity_uuid VARCHAR(36) UNIQUE,

    owner_player_id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NOT NULL,

    type TEXT NOT NULL,

    level INTEGER NOT NULL DEFAULT 1,
    xp INTEGER NOT NULL DEFAULT 0,

    attack_mode INTEGER NOT NULL DEFAULT 0,

    group_id VARCHAR(36),

    world VARCHAR(255),

    x REAL,
    y REAL,
    z REAL,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (owner_player_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    FOREIGN KEY (group_id)
        REFERENCES soldier_groups(id)
        ON DELETE SET NULL,

    CHECK (level BETWEEN 1 AND 7),
    CHECK (xp >= 0),
    CHECK (attack_mode BETWEEN 0 AND 5),
    CHECK (type IN ('infantry', 'archer', 'cavalry'))
);

-- ============================================================
-- Player Statistics
-- ============================================================

CREATE TABLE IF NOT EXISTS player_stats (
    player_id VARCHAR(36) PRIMARY KEY,

    kills INTEGER NOT NULL DEFAULT 0,
    deaths INTEGER NOT NULL DEFAULT 0,

    soldier_kills INTEGER NOT NULL DEFAULT 0,
    monster_kills INTEGER NOT NULL DEFAULT 0,

    playtime_seconds INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    CHECK (kills >= 0),
    CHECK (deaths >= 0),
    CHECK (soldier_kills >= 0),
    CHECK (monster_kills >= 0),
    CHECK (playtime_seconds >= 0)
);

-- ============================================================
-- Player Inventories
--
-- inventory_type examples:
--   enderchest
--
-- item_data contains the serialized Minecraft item.
-- ============================================================

CREATE TABLE IF NOT EXISTS inventories (
    owner_id VARCHAR(36) NOT NULL,
    inventory_type VARCHAR(64) NOT NULL,
    slot INTEGER NOT NULL,

    item_data TEXT,

    PRIMARY KEY (owner_id, inventory_type, slot),

    FOREIGN KEY (owner_id)
        REFERENCES players(id)
        ON DELETE CASCADE,

    CHECK (slot >= 0)
);

-- ============================================================
-- Team Inventories
--
-- inventory_type examples:
--   teamchest
-- ============================================================

CREATE TABLE IF NOT EXISTS team_inventories (
    team_id VARCHAR(36) NOT NULL,
    inventory_type VARCHAR(64) NOT NULL,
    slot INTEGER NOT NULL,

    item_data TEXT,

    PRIMARY KEY (team_id, inventory_type, slot),

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (slot >= 0)
);

-- ============================================================
-- Homes
-- ============================================================

CREATE TABLE IF NOT EXISTS homes (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    name VARCHAR(32) NOT NULL,
    world VARCHAR(255) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw DOUBLE NOT NULL,
    pitch DOUBLE NOT NULL,
    created_at BIGINT NOT NULL,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_homes_player_name
ON homes(player_id, name);

-- ============================================================
-- Death Points
-- ============================================================

CREATE TABLE IF NOT EXISTS death_points (
    id VARCHAR(36) PRIMARY KEY,

    player_id VARCHAR(36) NOT NULL,

    world VARCHAR(255) NOT NULL,

    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,

    yaw REAL NOT NULL DEFAULT 0,
    pitch REAL NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,

    inventory_data TEXT,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);

-- ============================================================
-- Mines
-- ============================================================

CREATE TABLE IF NOT EXISTS mines (
    id VARCHAR(36) PRIMARY KEY,

    owner_team_id VARCHAR(36) NOT NULL,

    world VARCHAR(255) NOT NULL,

    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,

    trigger_mode INTEGER NOT NULL DEFAULT 0,

    armed INTEGER NOT NULL DEFAULT 1,

    group_id VARCHAR(36),

    rearm_at INTEGER,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (owner_team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (trigger_mode BETWEEN 0 AND 2),
    CHECK (armed IN (0, 1))
);

-- ============================================================
-- Mine Groups
-- ============================================================

CREATE TABLE IF NOT EXISTS mine_groups (
    id VARCHAR(36) PRIMARY KEY,

    owner_team_id VARCHAR(36) NOT NULL,

    name VARCHAR(255) NOT NULL,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (owner_team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    UNIQUE (owner_team_id, name)
);

-- Add mine group foreign key after group table exists.
-- SQLite does not support adding a FK constraint directly,
-- therefore the application must validate group ownership.

-- ============================================================
-- Tax History
-- ============================================================

CREATE TABLE IF NOT EXISTS tax_transactions (
    id VARCHAR(36) PRIMARY KEY,

    team_id VARCHAR(36) NOT NULL,

    villager_count INTEGER NOT NULL,
    tax_bonus INTEGER NOT NULL,

    emeralds_charged INTEGER NOT NULL,

    successful INTEGER NOT NULL DEFAULT 1,

    reason TEXT,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE,

    CHECK (villager_count >= 0),
    CHECK (tax_bonus >= 1),
    CHECK (emeralds_charged >= 0),
    CHECK (successful IN (0, 1))
);

-- ============================================================
-- Generic Persistent Settings
-- ============================================================

CREATE TABLE IF NOT EXISTS settings (
    `key` VARCHAR(255) PRIMARY KEY,
    value TEXT
);

-- ============================================================
-- Useful indexes
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_players_team
    ON players(team_id);

CREATE INDEX IF NOT EXISTS idx_players_last_seen
    ON players(last_seen);

CREATE INDEX IF NOT EXISTS idx_diplomacy_other_team
    ON team_diplomacy(other_team_id);

CREATE INDEX IF NOT EXISTS idx_bonus_team
    ON team_bonus_sources(team_id);

CREATE INDEX IF NOT EXISTS idx_claims_team
    ON claims(team_id);

CREATE INDEX IF NOT EXISTS idx_claims_world
    ON claims(world);

CREATE INDEX IF NOT EXISTS idx_outposts_owner
    ON outposts(owner_team_id);

CREATE INDEX IF NOT EXISTS idx_tokens_round
    ON tokens(round_id);

CREATE INDEX IF NOT EXISTS idx_tokens_defeated
    ON tokens(defeated);

CREATE INDEX IF NOT EXISTS idx_soldiers_owner
    ON soldiers(owner_player_id);

CREATE INDEX IF NOT EXISTS idx_soldiers_team
    ON soldiers(team_id);

CREATE INDEX IF NOT EXISTS idx_soldiers_group
    ON soldiers(group_id);

CREATE INDEX IF NOT EXISTS idx_mines_team
    ON mines(owner_team_id);

CREATE INDEX IF NOT EXISTS idx_mines_world_position
    ON mines(world, x, y, z);

CREATE INDEX IF NOT EXISTS idx_tax_transactions_team
    ON tax_transactions(team_id);

CREATE INDEX IF NOT EXISTS idx_tax_transactions_created
    ON tax_transactions(created_at);

COMMIT;
-- ============================================================
-- Moderation
-- ============================================================

CREATE TABLE IF NOT EXISTS moderation_punishments (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    player_name TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    moderator_id VARCHAR(36),
    moderator_name TEXT,
    created_at BIGINT NOT NULL,
    expires_at BIGINT,
    active INTEGER NOT NULL DEFAULT 1,
    revoked_at BIGINT,
    revoked_by VARCHAR(36),
    revoke_reason TEXT,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
    FOREIGN KEY (moderator_id) REFERENCES players(id) ON DELETE SET NULL,
    FOREIGN KEY (revoked_by) REFERENCES players(id) ON DELETE SET NULL,
    CHECK (type IN ('WARN', 'KICK', 'BAN', 'TEMPBAN')),
    CHECK (active IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_moderation_player
    ON moderation_punishments(player_id);

CREATE INDEX IF NOT EXISTS idx_moderation_active
    ON moderation_punishments(active);

CREATE INDEX IF NOT EXISTS idx_moderation_expires
    ON moderation_punishments(expires_at);


-- ============================================================
-- Persistent Inventory Snapshots
-- ============================================================

CREATE TABLE IF NOT EXISTS inventory_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    captured_at BIGINT NOT NULL,
    inventory_data TEXT NOT NULL,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inventory_snapshots_player
    ON inventory_snapshots(player_id);

CREATE INDEX IF NOT EXISTS idx_inventory_snapshots_captured
    ON inventory_snapshots(captured_at);


-- Persistent player state snapshots
CREATE TABLE IF NOT EXISTS player_state_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    captured_at BIGINT NOT NULL,
    health REAL,
    max_health REAL,
    experience REAL,
    level INTEGER,
    food REAL,
    saturation REAL,
    air INTEGER,
    max_air INTEGER,
    world VARCHAR(255),
    x DOUBLE,
    y DOUBLE,
    z DOUBLE,
    yaw DOUBLE,
    pitch DOUBLE,
    inventory_data TEXT NOT NULL,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_player_state_snapshots_player
    ON player_state_snapshots(player_id);
CREATE INDEX IF NOT EXISTS idx_player_state_snapshots_captured
    ON player_state_snapshots(captured_at);
