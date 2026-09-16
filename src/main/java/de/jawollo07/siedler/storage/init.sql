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
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
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
    id TEXT PRIMARY KEY,
    last_name TEXT NOT NULL,

    team_id TEXT,

    eliminated INTEGER NOT NULL DEFAULT 0,

    first_join BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,

    FOREIGN KEY (team_id)
        REFERENCES teams(id)
        ON DELETE SET NULL,

    CHECK (eliminated IN (0, 1))
);
CREATE TABLE IF NOT EXISTS chat_messages (
    id TEXT PRIMARY KEY,
    player_id TEXT NOT NULL,
    player_name TEXT NOT NULL,
    world TEXT NOT NULL,
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
    team_id TEXT NOT NULL,
    other_team_id TEXT NOT NULL,

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

-- ============================================================
-- Tax Bonus Sources
-- ============================================================

CREATE TABLE IF NOT EXISTS team_bonus_sources (
    id TEXT PRIMARY KEY,

    team_id TEXT NOT NULL,

    source_type TEXT NOT NULL,
    source_id TEXT NOT NULL,

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
    id TEXT PRIMARY KEY,

    team_id TEXT NOT NULL,

    world TEXT NOT NULL,

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
    id TEXT PRIMARY KEY,

    name TEXT NOT NULL UNIQUE,

    world TEXT NOT NULL,

    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,

    radius INTEGER NOT NULL DEFAULT 12,

    owner_team_id TEXT,

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
    id TEXT PRIMARY KEY,

    started_at BIGINT NOT NULL,

    completed_at BIGINT,

    completed INTEGER NOT NULL DEFAULT 0,

    CHECK (completed IN (0, 1))
);

-- ============================================================
-- Token Monsters
-- ============================================================

CREATE TABLE IF NOT EXISTS tokens (
    id TEXT PRIMARY KEY,

    round_id TEXT NOT NULL,

    entity_uuid TEXT UNIQUE,

    world TEXT NOT NULL,

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
-- Soldier Groups
-- ============================================================

CREATE TABLE IF NOT EXISTS soldier_groups (
    id TEXT PRIMARY KEY,

    owner_player_id TEXT NOT NULL,

    team_id TEXT NOT NULL,

    name TEXT NOT NULL,

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
    id TEXT PRIMARY KEY,

    entity_uuid TEXT UNIQUE,

    owner_player_id TEXT NOT NULL,
    team_id TEXT NOT NULL,

    type TEXT NOT NULL,

    level INTEGER NOT NULL DEFAULT 1,
    xp INTEGER NOT NULL DEFAULT 0,

    attack_mode INTEGER NOT NULL DEFAULT 0,

    group_id TEXT,

    world TEXT,

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
    player_id TEXT PRIMARY KEY,

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
    owner_id TEXT NOT NULL,
    inventory_type TEXT NOT NULL,
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
    team_id TEXT NOT NULL,
    inventory_type TEXT NOT NULL,
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
    player_id TEXT NOT NULL,
    name TEXT NOT NULL,

    world TEXT NOT NULL,

    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,

    yaw REAL NOT NULL DEFAULT 0,
    pitch REAL NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,

    PRIMARY KEY (player_id, name),

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);

-- ============================================================
-- Death Points
-- ============================================================

CREATE TABLE IF NOT EXISTS death_points (
    id TEXT PRIMARY KEY,

    player_id TEXT NOT NULL,

    world TEXT NOT NULL,

    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,

    yaw REAL NOT NULL DEFAULT 0,
    pitch REAL NOT NULL DEFAULT 0,

    created_at INTEGER NOT NULL,

    FOREIGN KEY (player_id)
        REFERENCES players(id)
        ON DELETE CASCADE
);

-- ============================================================
-- Mines
-- ============================================================

CREATE TABLE IF NOT EXISTS mines (
    id TEXT PRIMARY KEY,

    owner_team_id TEXT NOT NULL,

    world TEXT NOT NULL,

    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,

    trigger_mode INTEGER NOT NULL DEFAULT 0,

    armed INTEGER NOT NULL DEFAULT 1,

    group_id TEXT,

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
    id TEXT PRIMARY KEY,

    owner_team_id TEXT NOT NULL,

    name TEXT NOT NULL,

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
    id TEXT PRIMARY KEY,

    team_id TEXT NOT NULL,

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
    key TEXT PRIMARY KEY,
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