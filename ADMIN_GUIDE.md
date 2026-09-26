# Siedler 2.0 – Admin-Handbuch

Dieses Handbuch beschreibt die administrativen Funktionen des aktuellen **Siedler-PNX beta-Branches**.

## 1. Voraussetzungen

Administrative Routen benötigen grundsätzlich:

```
siedler.admin
```

Spielerfunktionen besitzen eigene Berechtigungen wie beispielsweise `siedler.command.team`, `siedler.command.claim`, `siedler.command.eco`, `siedler.command.diplomatie`, `siedler.command.market` und `siedler.command.raid`.

## 2. Zentrale Verwaltung

```
/verwaltung
/verwaltung menu
/verwaltung help
```

Das Verwaltungsmenü bietet aktuell unter anderem:

- Online-Spieler
- Spielerinformationen
- Moderation
- Moderationshistorie
- Todeshistorie
- Todesinventare
- persönliche Enderchests
- Team-Enderchests

## 3. Spielerverwaltung und Moderation

Über `/verwaltung → Spieler` kann ein Online-Spieler ausgewählt werden.

Die Detailansicht enthält unter anderem Name, UUID, Welt, Position, Team, Bannstatus, Moderationseinträge und Todesdaten.

Verfügbare Maßnahmen:

- WARN
- KICK
- BAN
- TEMPBAN

Temporäre Bannzeiten umfassen aktuell:

- 30 Minuten
- 2 Stunden
- 24 Stunden

Moderationsmaßnahmen werden mit Spieler, Moderator, Zeitpunkt, Grund und Status persistent gespeichert.

### Bann aufheben

Aktive Banns können über die Verwaltungsoberfläche deaktiviert werden.

## 4. Moderations- und Todeshistorie

Die Moderationshistorie enthält Typ, Grund, Moderator, Erstellungszeit, Ablaufzeit und Status.

Die Todeshistorie enthält insbesondere:

- Datum/Uhrzeit
- Welt
- X/Y/Z
- Rotation
- gespeichertes Inventar

Ein Todesinventar kann über die Verwaltungsoberfläche eingesehen werden.

## 5. Teams

```
/team admin help
/team admin create <Name> <Farbe>
/team admin delete <Name>
/team admin add <Spieler> <Team>
/team admin remove <Spieler>
/team admin setcolor <Name> <Farbe>
```

Informationen:

```
/team list
/team info <Team>
```

## 6. Claims

Spieler dürfen nur `/claim help` und `/claim info` als normale Claim-Routen verwenden. Verwaltungsaktionen liegen unter `admin`.

```
/claim admin help
/claim admin set <Team>
/claim admin delete
/claim info
```

Claims schützen Teamgebiete. Normale feindliche Monster spawnen innerhalb eines Claims nicht.

## 7. Diplomatie

```
/diplomatie admin help
/diplomatie admin set <TeamA> <TeamB> <Beziehung>
/diplomatie admin show
/diplomatie admin show <Team>
/diplomatie admin list
```

Beziehungen:

```
allied
neutral
enemy
```

Die Beziehung wird für beide Richtungen gespeichert.

## 8. Wirtschaft und Steuern

Teamkonto:

```
/eco show <Team>
```

Steuerstatistik:

```
/eco stats
/eco stats <Team>
```

Administrative Kontenänderungen:

```
/eco admin set <Team> <Betrag>
/eco admin add <Team> <Betrag>
/eco admin remove <Team> <Betrag>
```

### Steuerformel

```
Coins = Dorfbewohner × TaxBonus × Rate
```

Standard:

- Rate: 1 Coin
- Intervall: 24 Stunden
- mindestens ein Teammitglied online

Eliminierte Teams werden bei der Steuerberechnung übersprungen.

## 9. Anti-AFK

Konfiguration:

```yaml
antiafk:
  enabled: true
  timeout-minutes: 15
  warning-seconds: 60
  exempt-permission: siedler.admin
```

Das System prüft einmal pro Sekunde. Nur tatsächliche Positionsänderungen gelten als Aktivität; reine Kopfbewegungen setzen den Aktivitätszeitpunkt nicht zurück.

## 10. Spielerstatistiken und Daten

```
/stats
/stats admin
```

Die Admin-GUI kann globale und spielerbezogene Statistiken anzeigen, darunter Kills, Tode, K/D, Soldaten-Kills, Monster-Kills und Spielzeit.

Spielerdaten werden regelmäßig und bei wichtigen Lifecycle-Ereignissen gespeichert. Ein normaler Snapshot kann unter anderem Inventar, Gesundheit, Erfahrung, Hunger, Position und Blickrichtung enthalten.

## 11. Enderchest-Verwaltung

Über:

```
/verwaltung
→ Enderchests verwalten
```

können beide Enderchest-Typen verwaltet werden.

### Persönliche Enderchests

`Persönliche Enderchests` zeigt aktuell online befindliche Spieler. Nach Auswahl wird die native **27-Slot-Enderchest** des Spielers geöffnet.

Der Administrator kann Items lesen, hinzufügen, entfernen und verschieben.

### Team-Enderchests

`Team-Enderchests` zeigt die vorhandenen Teams. Nach Auswahl wird die gemeinsame **54-Slot-Team-Enderchest** geöffnet.

Änderungen werden persistent gespeichert und sind für alle Teammitglieder unmittelbar sichtbar.

> Die aktuelle Verwaltung persönlicher Enderchests setzt voraus, dass der Zielspieler online ist.

## 12. Token-System

Token-Monster sind besondere Siedler-Begegnungen.

Adminbefehle:

```
/token admin help
/token admin start
/token admin spawn
/token admin status
```

Das System unterstützt:

- persistente Token-Runden
- konfigurierbare maximale aktive Token
- automatische Spawns
- sicheren Boden-Spawn
- Token-Belohnungen
- dauerhaften TaxBonus für das Team des Spielers, der ein Token besiegt

Konfiguration:

```yaml
monsters:
  token:
    enabled: true
    automatic: false
    max-active: 4
    entity: minecraft:zombie
    spawn-radius: 16
    spawn-interval-minutes: 30
    tax-bonus: 1
```

Token-Spawns umgehen bewusst die allgemeine Monstersteuerung.

## 13. Outpost-System

Adminbefehle:

```
/outpost admin help
/outpost admin create <Name>
/outpost admin delete <Name>
```

Spieler:

```
/outpost list
/outpost info <Name>
```

Konfiguration:

```yaml
monsters:
  outposts:
    enabled: true
    capture-seconds: 10
    radius: 12
    tax-bonus: 1
```

Regeln:

- Outposts besitzen einen festen Radius.
- Capture-Fortschritt läuft bei einem berechtigten Team.
- Bei gleichzeitiger Anwesenheit gegnerischer Teams pausiert der Fortschritt.
- Ohne Spieler im Bereich kann Fortschritt zurückgehen.
- Besitzer und Fortschritt werden persistent gespeichert.
- Ein besetzter Outpost gewährt seinem Team einen dauerhaften TaxBonus.
- Überlappende Outposts werden verhindert.
- Eliminierte Teams können keine Outposts erobern.

## 14. Pillager-Raids

Spieler:

```
/raid status
```

Admin:

```
/raid admin help
/raid admin start <Outpost>
/raid admin stop
```

Automatische Raids wählen einen besetzten Outpost mit mindestens einem online befindlichen Mitglied des verteidigenden Teams.

Konfiguration:

```yaml
monsters:
  raids:
    enabled: true
    automatic: true
    spawn-interval-minutes: 60
    waves: 3
    base-size: 3
    wave-growth: 2
    max-mobs-per-wave: 16
    spawn-radius: 20
    allow-vindicator: true
    allow-ravager: true
```

Raids bestehen aus mehreren persistent verwalteten Wellen. Je nach Welle können Pillager, Vindicator und Ravager erzeugt werden.

Aktive Raids werden beim Serverneustart sicher beendet, statt in einem unklaren Zustand fortgesetzt zu werden.

Raid-Spawns umgehen die allgemeine Monstersteuerung bewusst.

## 15. Allgemeine Monstersteuerung

Die normale Monstersteuerung erfolgt zentral über `MonsterManager`.

Beispiel:

```yaml
monsters:
  control:
    enabled: true
    monsters: []
    blacklist:
      - minecraft:creeper
    quotas:
      - minecraft:zombie:100
      - minecraft:skeleton:100
      - minecraft:creeper:0
      - minecraft:enderman:75
      - minecraft:witch:50
    default-quota-percent: 100
    max-per-chunk: 12
    max-per-world: 200
    world-blacklist: []
```

### Regeln

- `enabled`: Steuerung aktivieren/deaktivieren.
- `monsters`: kontrollierte Entity-IDs; leer verwendet die eingebaute Liste normaler feindlicher Mobs.
- `blacklist`: vollständige Blockierung, unabhängig von der Quote.
- `quotas`: Spawnwahrscheinlichkeit von 0–100 %.
- `default-quota-percent`: Standardquote.
- `max-per-chunk`: Limit je kontrolliertem Mob und Chunk.
- `max-per-world`: Limit je kontrolliertem Mob und Welt.
- `world-blacklist`: Welten ohne kontrollierte Monster.

### Claims und Märkte

Normale feindliche Monster werden innerhalb von Siedler-Claims blockiert.

In konfigurierten Marktplätzen werden normale feindliche Monster ebenfalls blockiert. Bereits vorhandene Monster können über `/market admin cleanup` entfernt werden.

Token und Raid-Mobs besitzen einen ausdrücklichen Bypass, weil sie kontrollierte Siedler-Ereignisse sind.

## 16. Markt und Händler

Phase 7 ist implementiert.

### Marktverwaltung

```
/market help
/market info
/market types
```

Admin:

```
/market admin help
/market admin reload
/market admin cleanup
/market admin spawn <type>
```

Alle `admin`-Routen benötigen `siedler.admin`.

### Marktschutz

Konfigurierte Marktbereiche sind rechteckige Bereiche mit Welt, Grenzen und Händler-Spawnposition.

Dort sind standardmäßig:

- Blockabbau gesperrt
- Blockplatzierung gesperrt
- konfigurierte Interaktionen gesperrt
- normale feindliche Monster gesperrt

### Händler

Die Händler verwenden native PowerNukkitX-`VillagerV2`-Entities und eine Siedler-`SimpleForm`-Handelsoberfläche.

Trades können konfigurieren:

- erstes Kaufitem
- optionales zweites Kaufitem
- Verkaufsitem
- maximale Nutzungen
- Händlerstufe
- Händler-XP
- Preis-Multiplikator

Die Standardrollen umfassen:

- Lebensmittel
- Baustoffe
- Rohstoffe
- Werkzeuge
- Waffen
- Versorgung
- Soldaten
- Verzauberungen

Automatische Händlerpflege kann pro Markt eine konfigurierbare Anzahl jeder Händlerart aufrechterhalten.

Beispiel:

```yaml
market:
  enabled: true
  interaction-blacklist:
    - minecraft:stone_button
    - minecraft:oak_button
    - minecraft:lever
  markets:
    - id: markt
      enabled: true
      world: overworld
      min-x: -28
      min-z: 21
      max-x: -10
      max-z: 59
      trader-spawn:
        x: -19
        y: 106
        z: 40
  traders:
    automatic: true
    count-per-type: 1
```

## 17. Zentrale Admin-Befehle

### Verwaltung

```
/verwaltung
/verwaltung menu
/verwaltung help
```

### Teams

```
/team admin help
/team admin create <Name> <Farbe>
/team admin delete <Name>
/team admin add <Spieler> <Team>
/team admin remove <Spieler>
/team admin setcolor <Name> <Farbe>
```

### Claims

```
/claim admin help
/claim admin set <Team>
/claim admin delete
```

### Diplomatie

```
/diplomatie admin help
/diplomatie admin set <TeamA> <TeamB> <Beziehung>
/diplomatie admin show
/diplomatie admin show <Team>
/diplomatie admin list
```

### Wirtschaft

```
/eco show <Team>
/eco stats
/eco stats <Team>
/eco admin set <Team> <Betrag>
/eco admin add <Team> <Betrag>
/eco admin remove <Team> <Betrag>
```

### Monster und Events

```
/token admin help
/token admin start
/token admin spawn
/token admin status

/outpost admin help
/outpost admin create <Name>
/outpost admin delete <Name>

/raid status
/raid admin help
/raid admin start <Outpost>
/raid admin stop
```

### Markt

```
/market help
/market info
/market types
/market admin help
/market admin reload
/market admin cleanup
/market admin spawn <type>
```

## 18. Administrationsgrundsätze

### Nachvollziehbarkeit
Administrative Änderungen sollten nachvollziehbar bleiben.

### Moderation
Bei Maßnahmen immer einen passenden Grund auswählen und die bestehende Historie berücksichtigen.

### Wirtschaft
Manuelle Kontenänderungen nur durchführen, wenn sie erforderlich sind.

### Claims
Vor dem Setzen oder Löschen immer Position und Zielteam prüfen.

### Events
Token, Outposts und Raids besitzen eigene persistente Zustände. Bei manuellen Eingriffen sollte der aktuelle Status zuerst geprüft werden.

### Markt
Bei Marktänderungen auf Grenzen, Welt und Händler-Spawnposition achten. `cleanup` entfernt normale hostile Mobs aus den konfigurierten Märkten.

## 19. Datenbank und Migration

Die Datenbank wird beim Start automatisch auf den aktuellen Schema-Stand gebracht.

Neue persistente Systeme müssen bei späteren Änderungen über Schema-Migrationen erweitert werden; eine Änderung von `CREATE TABLE IF NOT EXISTS` allein verändert bestehende Tabellen nicht.

Vor größeren Server-Upgrades sollte die Datei bzw. Datenbank des Siedler-Systems gesichert werden.

## 20. Serverstart und Java 21

Siedler-PNX läuft mit Java 21. PowerNukkitX benötigt auf entsprechenden Java-21-Umgebungen die vom Server bereitgestellten `--add-opens`-Optionen.

Beispiel:

```bash
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  -jar powernukkitx.jar
```

Diese JVM-Optionen gehören zum Serverstart und sind keine Siedler-Konfiguration.

## 21. Release-/Wartungshinweise

Nach Änderungen am Plugin:

```
mvn clean package
```

Anschließend die erzeugte JAR aus `target/` auf dem Zielserver installieren und die Serverlogs auf:

- Datenbankmigration
- Command-Registrierung
- Event-/Listener-Registrierung
- Monster-/Event-Systeme
- Markt-/Trader-System

prüfen.

README und `plan.md` sollten bei neuen Phasen gemeinsam mit den Guides aktualisiert werden.


## PvP-Turniersystem

Der `beta`-Stand besitzt ein administrierbares PvP-Turniersystem.

### Spielersteuerung
```
/tournament join
/tournament leave
/tournament status
/tournament bracket
/tournament kits
/tournament kit <Name>
/tournament spectate
```

### Turnierverwaltung
```
/tournament admin open
/tournament admin start
/tournament admin stop
/tournament admin status
```
Alle Admin-, Kit-, Arena- und Konfigurationsrouten benötigen `siedler.admin`.

### Formate
- `single_elimination`: eine Niederlage scheidet aus.
- `double_elimination`: zwei Niederlagen sind erforderlich.
- `round_robin`: jeder spielt gegen jeden.

### Mehrere Matches
Mehrere Matches können gleichzeitig laufen. Pro Paarung wird eine Match-ID verwaltet; konfigurierte Arenen können parallel belegt werden.

### Arenen
```
/tournament admin arena list
/tournament admin arena create <Name>
/tournament admin arena delete <Name>
/tournament admin arena set <Arena> <Pfad> <Wert>
```

### Best-of und Zeitlimit
```yaml
tournament:
  format: single_elimination
  match:
    best-of: 3
    timeout-seconds: 600
```

### Kits
```
/tournament admin kit list
/tournament admin kit create <Name>
/tournament admin kit delete <Name>
/tournament admin kit clear <Name>
/tournament admin kit add <Name> <Command>
/tournament admin kit show <Name>
```

### Dynamische Konfiguration
```
/tournament admin config get <Pfad>
/tournament admin config set <Pfad> <Wert>
/tournament admin config reload
```

Nur Pfade unter `tournament.*` dürfen über diese Schnittstelle geändert werden.

---

**Dieses Handbuch beschreibt den aktuellen beta-Stand. Bei Änderungen an Commands, Berechtigungen, Konfiguration oder Gameplay-Regeln muss es entsprechend aktualisiert werden.**
