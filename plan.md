# Siedler 2.0 – Migration Plan

Stand: 15.09.2026

## Versionsschema

Die Versionsnummer ist direkt an die Phasen dieses Plans gekoppelt:

```text
PHASE.FEATURE.PATCH
```

- **PHASE** – entspricht der aktuellen Phase des Migrationsplans.
- **FEATURE** – nummeriert größere abgeschlossene Meilensteine innerhalb der Phase.
- **PATCH** – Bugfixes, Korrekturen und kleine technische Änderungen ohne neuen Meilenstein.
- Während der Entwicklung wird die Versionsnummer mit `-SNAPSHOT` ergänzt.
- Eine neue Phase startet grundsätzlich mit `X.0.0-SNAPSHOT`.
- Ein neuer funktionaler Meilenstein erhöht die mittlere Zahl: `X.0.0 → X.1.0`.
- Ein reiner Fix erhöht die letzte Zahl: `X.1.0 → X.1.1`.

Damit beschreibt die Versionsnummer jederzeit, **wo das Projekt im Migrationsplan steht**. Die Versionsnummer muss nicht für jeden einzelnen Checkbox-Punkt geändert werden; zusammengehörige Punkte können einen gemeinsamen Meilenstein bilden.

### Phasen-Zuordnung

| Version | Phase |
|---|---|
| `0.x.x` | Foundation |
| `1.x.x` | Core & Datenmodell |
| `2.x.x` | Teams |
| `3.x.x` | Claims |
| `4.x.x` | Economy & Taxes |
| `5.x.x` | Monster, Tokens & Outposts |
| `6.x.x` | Soldiers |
| `7.x.x` | Market & Traders |
| `8.x.x` | Essentials & Statistics |
| `9.x.x` | Minefield |
| `10.x.x` | Migration & Release |

Der erste stabile Gesamt-Release ist damit `10.0.0`.

## Phase 0 – Foundation ✅

- [x] Neues PowerNukkitX Repository initialisieren
- [x] Java-21 Grundstruktur mit Maven
- [x] Plugin-Lifecycle (`SiedlerPlugin`)
- [x] zentrale Manager-Struktur
- [x] SQLite Storage-Grundlage
- [x] MariaDB JDBC-Unterstützung
- [x] Konfiguration
- [x] `/siedler` Basiscommand
- [x] README und Plan anlegen

## Phase 1 – Core & Datenmodell

- [x] Storage Verwaltung
- [x] Player-ID Datenmodell

## Phase 2 – Teams

- [x] TeamManager
- [x] Team-Erstellung/Löschen
- [x] Team-Mitglieder und stabile Player-IDs
- [x] Teamchat
- [x] Diplomatie: allied / neutral / enemy
- [x] Team-Eliminierung
- [x] permanenter Spectator nach Tod eines eliminierten Spielers
- [x] konfigurierbarer Eliminierungsblock

### Versionsmeilensteine Phase 2

- `2.0.0` – TeamManager-Grundlage
- `2.1.0` – Team-Erstellung und -Löschung
- `2.2.0` – Team-Mitglieder und stabile Player-IDs
- `2.3.0` – Teamfarben und Anzeige
- `2.4.0` – Teamchat
- `2.5.0` – Diplomatie
- `2.6.0` – Team-Eliminierung
- `2.7.0` – permanenter Spectator nach Eliminierung
- `2.8.0` – konfigurierbarer Eliminierungsblock

## Phase 3 – Claims

- [x] Claim-Datenmodell
- [x] Claim erstellen/verwalten
- [x] Block-Break-Schutz
- [x] Block-Place-Schutz
- [ ] Claim-Grenzen/Visualisierung
- [ ] Claim-sicheres Monster-/Pillager-Spawning

## Phase 4 – Economy & Taxes

- [x] Economy-Service
- [ ] Villager-basierte Tagessteuer
- [ ] Online-Team-Bedingung
- [ ] TaxBonusManager
- [x] `taxBonus=1` als Standard für bestehende/neue Teams
- [ ] Token-Bonus als permanente TaxBonus-Quelle
- [ ] Outpost-Bonus als permanente TaxBonus-Quelle
- [ ] atomare Buchung + Retry
- [ ] Steuerstatistiken

## Phase 5 – Monster, Tokens & Outposts

- [ ] TokenManager
- [ ] automatische Token-Spawns per Konfiguration/Befehl
- [ ] maximal aktive Token-Monster
- [ ] sichere Spawnpositionen
- [ ] Token-Runden und persistenter Abschlussstatus
- [ ] Token-Besiegung → TaxBonus
- [ ] Outpost-Registrierung
- [ ] Outpost-Capture mit Radius und Capture-Zeit
- [ ] Contesting zwischen Teams
- [ ] persistenter Outpost-Besitzer
- [ ] Outpost-Capture → TaxBonus
- [ ] Pillager Squads / Raids
- [ ] normale Monster und Siedler-Monstersteuerung

## Phase 6 – Market & Traders

- [ ] MarketManager
- [ ] Market-Schutz gegen Break + Place
- [ ] Monster-Deaktivierung im Markt
- [ ] TraderManager
- [ ] frei definierbare Trades
- [ ] vordefinierte Siedler-3-artige Trader
- [ ] Emerald als Standardwährung
- [ ] Soldier Trader
- [ ] weitere Spezial-Trader

## Phase 7 – Essentials & Statistics

- [ ] Homes
- [ ] TPA
- [ ] Start-System
- [ ] Death Points
- [ ] persistenter 27-Slot `/ec`
- [ ] persistenter 54-Slot `/teamchest`
- [ ] Player Stats
- [ ] Server Dashboard/Statistiken
- [ ] Anti-AFK

## Phase 8 – Minefield

- [ ] Mine-Item
- [ ] persistente Minen
- [ ] Minegruppen
- [ ] Platzierungsvalidierung
- [ ] Trigger-Modi
- [ ] Warnung/Sound
- [ ] verzögerte Detonation
- [ ] Kettenreaktionen
- [ ] Explosion ohne Blockschaden + Feuer
- [ ] automatisches Wieder-Scharfmachen
- [ ] Control-System
- [ ] Team-/Diplomatie-Integration

## Phase 9 – Soldiers

- [ ] Soldier-Datenmodell und Registry
- [ ] Besitzer/Team über Player-ID
- [ ] Infantry
- [ ] Archer
- [ ] Cavalry
- [ ] Level 1–7 + XP
- [ ] Equipment
- [ ] Gruppen
- [ ] Spawn-/Teleport-Commands
- [ ] Attack modes 0–5
- [ ] native PNX AI
- [ ] Targeting anhand Diplomatie
- [ ] Pathfinding/A*
- [ ] Stuck-Recovery und Detours
- [ ] Cavalry Charge/Pass/Jumps
- [ ] Archer-Ballistik
- [ ] Combat positions/Formationen
- [ ] Hit-/Attack-Handling

## Phase 10 – Migration & Release

- [ ] vollständige Command-Dokumentation / USER_GUIDE
- [ ] Server-Test auf Zielversion
- [ ] Performance-Test unter Last
- [ ] Persistenz-/Restart-Tests
- [ ] saubere Fehlerbehandlung
- [ ] Release-Build

## Aktueller Stand

**Aktuelle Planphase:** Phase 2 – Teams

Die Foundation und die grundlegenden Core-/Datenmodell-Arbeiten sind abgeschlossen. In Phase 2 sind TeamManager, Team-Erstellung/Löschung, Team-Mitglieder mit stabilen Player-IDs sowie Teamfarben/Anzeige umgesetzt.

Der nächste noch offene funktionale Meilenstein ist **`2.4.0` – Teamchat**.

Der Build läuft über **Maven** mit **Java 21** und verwendet PowerNukkitX `org.powernukkitx:server:3.0.4-SNAPSHOT` als `provided`-Dependency.
