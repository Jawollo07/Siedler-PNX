# Siedler 2.0 – Migration Plan

Stand: 23.09.2026

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

## Phase 2 – Teams ✅

- [x] TeamManager
- [x] Team-Erstellung/Löschen
- [x] Team-Mitglieder und stabile Player-IDs
- [x] Teamfarben und Anzeige
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

## Command-System

Die migrierten Commands verwenden die **PowerNukkitX Tree Command API**.

- [x] zentrale Command-Registrierung über `CommandManager`
- [x] Tree-API-Routing für ClaimCommand
- [x] Tree-API-Routing für TeamCommand
- [x] Tree-API-Routing für RelationCommands
- [x] Tree-API-Routing für Elimination
- [x] Tree-API-Routing für EcoCommand
- [x] administrative Routen unter einem expliziten `admin`-Subcommand
- [x] Schutz administrativer Routen mit `siedler.admin`
- [x] normale Kommunikationscommands bleiben unabhängig von `admin`

### Aktuelle Command-Struktur

| Command | Spieler | Admin (`siedler.admin`) |
|---|---|---|
| `/claim` | `help`, `info` | `admin help`, `admin set`, `admin delete` |
| `/team` | `help`, `list`, `info <name>` | `admin help`, `admin create`, `admin delete`, `admin add`, `admin remove`, `admin setcolor` |
| `/diplomatie` | `help`, `set`, `show` | `admin help`, `admin set`, `admin show`, `admin list` |
| `/elimination` | `help`, `list` | `admin help`, `admin eliminate`, `admin deeliminate` |
| `/eco` | `help`, `show`, `stats [Team]` | `admin help`, `admin set`, `admin add`, `admin remove` |

`/dm` und `/teamchat` sind Kommunikationscommands und benötigen keine `siedler.admin`-Struktur.

## Phase 3 – Claims

- [x] Claim-Datenmodell
- [x] Claim erstellen/verwalten
- [x] Claim-Berechtigungs-/Teamprüfung
- [x] Block-Break-Schutz
- [x] Block-Place-Schutz
- [x] Claim-Adminbefehle über `/claim admin`
- [ ] Claim-Grenzen/Visualisierung
- [ ] Claim-sicheres Monster-/Pillager-Spawning

## Phase 4 – Economy & Taxes

- [x] Economy-Service
- [x] Villager-basierte Tagessteuer
- [x] Online-Team-Bedingung
- [x] `taxBonus=1` als Standard für bestehende/neue Teams
- [ ] Token-Bonus als permanente TaxBonus-Quelle
- [ ] Outpost-Bonus als permanente TaxBonus-Quelle
- [x] atomare Buchung + Retry
- [x] Steuertransaktionen / Steuerhistorie
- [x] Steuerstatistik

### Tax-System

Die aktuelle Steuerformel lautet:

```text
Coins = Dorfbewohner × TaxBonus × Rate
```

Bei der Standard-Konfiguration `Rate = 1` gilt beispielsweise:

```text
1 Dorfbewohner × TaxBonus 1 = 1 Coin
1 Dorfbewohner × TaxBonus 3 = 3 Coins
10 Dorfbewohner × TaxBonus 3 = 30 Coins
```

Die Steuer ist **Team-Einkommen** und wird der Team-Balance gutgeschrieben. Sie wird nur ausgeführt, wenn mindestens ein Mitglied des Teams online ist. Ausgeschiedene Teams erhalten keine Steuer.

Die Steuer wird standardmäßig alle 24 Stunden geprüft. Fehlgeschlagene Steuerläufe werden protokolliert und können über das konfigurierte Retry-Intervall erneut versucht werden. Mit `/eco stats [Team]` sind Steuerstatistiken als Ingame-SimpleForm verfügbar; enthalten sind Gesamteinnahmen, erfolgreiche/fehlgeschlagene Erhebungen, erfasste Dorfbewohner und die letzte Steuererhebung.

## Phase 5 – Essentials & Statistics

- [x] Homes
- [x] `/sethome <Name>`
- [x] `/home <Name>`
- [x] `/homes` mit SimpleForm-GUI
- [x] `/delhome <Name>`
- [x] persistente Home-Speicherung mit Welt, Position und Rotation
- [x] Ownership-Prüfung für Home-Zugriff und Löschung
- [x] zentrale Home-Nachrichten über `MessageManager`
- [x] Management-/Moderations-GUI
- [x] Warn, Kick, Ban und TempBan
- [x] Unban und Moderationshistorie
- [x] persistente Moderationsdaten
- [x] TPA-Anfragen
- [x] `/tpa <Spieler>`
- [x] `/tpaccept`
- [x] `/tpdeny`
- [x] `/tpacancel`
- [x] automatische Anfrage-Ablaufzeit (60 Sekunden)
- [x] nur eine offene Anfrage pro Absender/Ziel
- [ ] Start-System
- [x] Death Points
- [x] `/death` Teleport zum letzten Todespunkt
- [x] Persistenter letzter Todespunkt mit Welt, Position und Rotation
- [x] Todespunkt wird nach erfolgreicher Nutzung entfernt
- [x] persistenter 27-Slot `/ec`
- [x] persistenter 54-Slot `/tec` / Team-Enderchest
- [x] Admin-Verwaltung für persönliche Enderchests (Lesen/Schreiben)
- [x] Admin-Verwaltung für Team-Enderchests (Lesen/Schreiben)
- [x] Player Stats (`/stats` und `/stats admin`)
- [x] Anti-AFK mit konfigurierbarer Inaktivitätszeit, Warnung und Admin-Ausnahme

### Home-System

Das Home-System verwendet die persistente `homes`-Tabelle. Ein Home speichert:

```text
ID
Player-ID
Name
Welt
X / Y / Z
Yaw / Pitch
Erstellzeitpunkt
```

Spieler können nur ihre eigenen Homes lesen, teleportieren und löschen. Home-Namen sind auf 32 Zeichen begrenzt und pro Spieler eindeutig.

### Management & Moderation

Die Verwaltung ist über `/verwaltung` erreichbar und besitzt eine GUI für Online-Spieler, Spielerdetails und Moderationsaktionen. Moderationsmaßnahmen werden dauerhaft in `moderation_punishments` gespeichert und können über die Historie nachvollzogen werden.

Unterstützte Maßnahmen:

- Warn
- Kick
- permanenter Ban
- temporärer Ban
- Unban
- Moderationshistorie

## Phase 6 – Monster, Tokens & Outposts

- [x] TokenManager
- [x] automatische Token-Spawns per Konfiguration/Befehl
- [x] maximal aktive Token-Monster
- [x] sichere Spawnpositionen
- [x] Token-Runden und persistenter Abschlussstatus
- [x] Token-Besiegung → TaxBonus
- [ ] Outpost-Registrierung
- [ ] Outpost-Capture mit Radius und Capture-Zeit
- [ ] Contesting zwischen Teams
- [ ] persistenter Outpost-Besitzer
- [ ] Outpost-Capture → TaxBonus
- [ ] Pillager Squads / Raids
- [ ] normale Monster und Siedler-Monstersteuerung

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

**Aktuelle Planphase:** Phase 6 – Monster, Tokens & Outposts

Phase 0 und Phase 1 sind abgeschlossen. Phase 2 – Teams ist abgeschlossen, einschließlich Teamchat, Diplomatie, Eliminierung und der zugehörigen Spectator-/Eliminierungsregeln.

Phase 3 – Claims ist begonnen. Claim-Datenmodell, Claim-Verwaltung, Team-/Berechtigungsprüfungen sowie Block-Break- und Block-Place-Schutz sind umgesetzt. Claim-Visualisierung und claim-sicheres Monster-/Pillager-Spawning bleiben offen.

Phase 4 – Economy & Taxes ist weit fortgeschritten. Die Economy verwendet `team_money` als zentrale Balancequelle. Transaktionale Balanceänderungen, Balance-Historie und die villagerbasierte Tagessteuer sind umgesetzt. Die Steuer berechnet `Dorfbewohner × TaxBonus × Rate`, schreibt das Ergebnis als Team-Einkommen gut und setzt mindestens ein online befindliches Teammitglied voraus.

Der Build läuft über **Maven** mit **Java 21** und verwendet PowerNukkitX `org.powernukkitx:server:3.0.4-SNAPSHOT` als `provided`-Dependency.


Die Phase 5 – Essentials & Statistics wurde mit dem Home-System und dem TPA-System und der zentralen Verwaltungs-/Moderationsoberfläche begonnen. Homes sind persistent und über eigene Tree-Command-Commands sowie eine SimpleForm-GUI nutzbar. Die Moderation unterstützt Warnungen, Kicks, permanente und temporäre Bans sowie Unbans und eine persistente Historie. TPA, Start-System, Death Points, persistente Ender-/Teamchests mit Admin-Verwaltung, Player Stats und Anti-AFK sind umgesetzt. Phase 6 wurde mit dem Token-System begonnen; Outposts, Capture/Contesting, Raids und die allgemeine Monstersteuerung bleiben offen.


### TPA-System

Das TPA-System arbeitet mit kurzlebigen, serverseitigen Anfragen. Eine Anfrage ist standardmäßig 60 Sekunden gültig. Pro Spieler kann jeweils nur eine ausgehende bzw. eingehende Anfrage aktiv sein. Annahme teleportiert den anfragenden Spieler zum Zielspieler; Ablehnung und Abbruch entfernen die Anfrage. Offline-/abgelaufene Anfragen werden verworfen.

### Death System

Das Death-System speichert Todespunkte eines Spielers persistent in `death_points`. Mit `/death` kann der Spieler zum letzten Punkt zurückkehren; die Punkte bleiben für die Admin-Todeshistorie erhalten.
