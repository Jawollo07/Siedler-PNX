# Siedler 2.0 – Migration Plan

Stand: 11.09.2026

## Phase 0 – Foundation ✅

- [x] Neues PowerNukkitX Repository initialisieren
- [x] Maven/Java-21 Grundstruktur
- [x] Plugin-Lifecycle (`SiedlerPlugin`)
- [x] zentrale Manager-Struktur
- [x] SQLite Storage-Grundlage
- [x] Konfiguration
- [x] `/siedler` Basiscommand
- [x] README und Plan anlegen

## Phase 1 – Core & Datenmodell

- [ ] Storage-Migration auf versioniertes Schema
- [ ] Repository/DAO-Abstraktionen
- [ ] Player-ID/UUID-Datenmodell
- [ ] zentraler Task-/Scheduler-Service
- [ ] strukturierter Logger und Debug-Level
- [ ] API-Kompatibilitätsguards

## Phase 2 – Teams

- [ ] TeamManager
- [ ] Team-Erstellung/Löschen
- [ ] Team-Mitglieder und stabile Player-IDs
- [ ] Teamfarben/Anzeige
- [ ] Teamchat
- [ ] Diplomatie: allied / neutral / enemy
- [ ] Team-Eliminierung
- [ ] permanenter Spectator nach Tod eines eliminierten Spielers
- [ ] konfigurierbarer Eliminierungsblock

## Phase 3 – Claims

- [ ] Claim-Datenmodell
- [ ] Claim erstellen/verwalten
- [ ] Block-Break-Schutz
- [ ] Block-Place-Schutz
- [ ] Claim-Grenzen/Visualisierung
- [ ] Block-Recovery
- [ ] Online-Defender-Prüfung für Angriffe
- [ ] Claim-sicheres Monster-/Pillager-Spawning

## Phase 4 – Economy & Taxes

- [ ] Economy-Service
- [ ] Villager-basierte Tagessteuer
- [ ] Online-Team-Bedingung
- [ ] TaxBonusManager
- [ ] `taxBonus=1` als Standard für bestehende/neue Teams
- [ ] Token-Bonus als permanente TaxBonus-Quelle
- [ ] Outpost-Bonus als permanente TaxBonus-Quelle
- [ ] gemeinsames Bonus-Limit
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

## Phase 6 – Soldiers

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

## Phase 7 – Market & Traders

- [ ] MarketManager
- [ ] Market-Schutz gegen Break + Place
- [ ] Monster-Deaktivierung im Markt
- [ ] TraderManager
- [ ] frei definierbare Trades
- [ ] vordefinierte Siedler-3-artige Trader
- [ ] Emerald als Standardwährung
- [ ] Soldier Trader
- [ ] weitere Spezial-Trader

## Phase 8 – Essentials & Statistics

- [ ] Homes
- [ ] TPA
- [ ] Start-System
- [ ] Death Points
- [ ] persistenter 27-Slot `/ec`
- [ ] persistenter 54-Slot `/teamchest`
- [ ] Player Stats
- [ ] Server Dashboard/Statistiken
- [ ] Anti-AFK

## Phase 9 – Minefield

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

## Phase 10 – Migration & Release

- [ ] Datenmigration aus Siedler 1.x, soweit sinnvoll möglich
- [ ] Konfigurationsmigration dokumentieren
- [ ] vollständige Command-Dokumentation / USER_GUIDE
- [ ] Server-Test auf Zielversion
- [ ] Performance-Test unter Last
- [ ] Persistenz-/Restart-Tests
- [ ] saubere Fehlerbehandlung
- [ ] Release-Build
- [ ] Migrationshinweise für den Umstieg von BDS Script API auf PNX

## Aktueller Fokus

**Als Nächstes:** Phase 1 – ein sauberes, versioniertes Datenmodell und die gemeinsamen Services, danach Teams. Der Build läuft jetzt über Maven und verwendet die offizielle PowerNukkitX-Maven-Koordinate `org.powernukkitx:server:3.0.4` als `provided` dependency.
