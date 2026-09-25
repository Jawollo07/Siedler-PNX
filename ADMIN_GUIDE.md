# Siedler 2.0 – Admin-Handbuch

Dieses Handbuch beschreibt die administrativen Funktionen des aktuellen **Siedler 2.0**-Systems.

> **Geltungsbereich:** Dieses Dokument bezieht sich auf den aktuellen **beta-Branch** von Siedler-PNX.

## 1. Voraussetzungen

Administrative Befehle benötigen grundsätzlich:

    siedler.admin

Wichtige Spieler-Berechtigungen sind unter anderem:

    siedler.command.team
    siedler.command.claim
    siedler.command.eco
    siedler.command.diplomatie
    siedler.command.stats

Die Admin-Berechtigung sollte nur vertrauenswürdigen Personen gegeben werden.

## 2. Verwaltungsmenü

Die zentrale Verwaltung:

    /verwaltung
    /verwaltung menu

Hilfe:

    /verwaltung help

Das Menü bietet aktuell:
- Online-Spieler
- Spielerverwaltung
- Moderation
- Moderationshistorie
- Todeshistorie
- Todesinventare
- Spielerinformationen
- persönliche Enderchests lesen und bearbeiten
- Team-Enderchests lesen und bearbeiten

## 3. Spielerverwaltung

Über **/verwaltung → Spieler** kann ein Online-Spieler ausgewählt werden.

Die Detailansicht zeigt:
- Spielername
- UUID
- Welt
- X/Y/Z-Position
- Team
- aktiven Bann
- Moderationseinträge
- gespeicherte Todespunkte

Von dort aus sind Moderation, Moderationshistorie und Todeshistorie erreichbar.

## 4. Moderation

Aktuelle Maßnahmen:
- Verwarnung (WARN)
- Kick (KICK)
- permanenter Bann (BAN)
- temporärer Bann (TEMPBAN)

Temporäre Banns:
- 30 Minuten
- 2 Stunden
- 24 Stunden

Ein aktiver Bann kann über **Bann aufheben** deaktiviert werden.

### Moderationsgründe

- Verstoß gegen die Serverregeln
- Belästigung / Beleidigung
- Cheating / Exploiting
- Unangemessenes Verhalten
- Sonstiger Regelverstoß

Jede Maßnahme wird mit Spieler, Moderator, Zeitpunkt, Grund und Status gespeichert.

## 5. Moderationshistorie

Ein Eintrag enthält:
- Typ
- Grund
- Moderator
- Erstellungszeit
- Ablaufzeit
- Status

Status:
- Aktiv
- Beendet

## 6. Bannsystem

Banns werden persistent in der Datenbank gespeichert. Beim Beitritt wird geprüft, ob ein aktiver Bann vorhanden ist.

Gespeichert werden unter anderem Spieler-ID, Spielername, Bannart, Grund, Moderator, Erstellungszeit, Ablaufzeit, Aktivstatus und Informationen zur Aufhebung.

## 7. Teams administrieren

Hilfe:

    /team admin help

Team erstellen:

    /team admin create <Name> <Farbe>

Team löschen:

    /team admin delete <Name>

Spieler hinzufügen:

    /team admin add <Spieler> <Team>

Spieler entfernen:

    /team admin remove <Spieler>

Teamfarbe ändern:

    /team admin setcolor <Name> <Farbe>

Informationen:

    /team list
    /team info <Team>

## 8. Claims administrieren

Hilfe:

    /claim admin help

Claim setzen:

    /claim admin set <Team>

Claim löschen:

    /claim admin delete

Claim prüfen:

    /claim info

Claims sollten sorgfältig verwaltet werden, da sie den geschützten Spielbereich eines Teams bestimmen.

## 9. Diplomatie administrieren

Hilfe:

    /diplomatie admin help

Beziehung setzen:

    /diplomatie admin set <TeamA> <TeamB> <Beziehung>

Mögliche Beziehungen:

    allied
    neutral
    enemy

Beispiel:

    /diplomatie admin set Rot Blau allied

Anzeigen:

    /diplomatie admin show
    /diplomatie admin show <Team>
    /diplomatie admin list

Die Beziehung wird für beide Richtungen gespeichert.

## 10. Wirtschaft administrieren

Teamkonto:

    /eco show <Team>

Steuerstatistik:

    /eco stats
    /eco stats <Team>

Adminänderungen:

    /eco admin set <Team> <Betrag>
    /eco admin add <Team> <Betrag>
    /eco admin remove <Team> <Betrag>

Beispiele:

    /eco admin set Rot 1000
    /eco admin add Rot 500
    /eco admin remove Rot 250

Manuelle Änderungen am Teamkonto sollten dokumentiert werden.

## 11. Steuersystem

Die automatische Teamsteuer basiert auf Dorfbewohnern.

    Steuer = Dorfbewohner × TaxBonus × Rate

Standard:
- Rate: 1 Coin
- Intervall: 24 Stunden

Besonderheiten:
- Eliminierte Teams werden übersprungen.
- Mindestens ein Teammitglied muss online sein.
- Dorfbewohner werden innerhalb der Team-Claims gezählt.
- Fehlgeschlagene Steuerzyklen werden protokolliert.
- Steuertransaktionen werden gespeichert.

## 12. Todeshistorie

Über:

    /verwaltung
    → Spieler
    → Todeshistorie

können alle gespeicherten Todespunkte eines Spielers eingesehen werden.

Ein Todespunkt enthält:
- Spieler
- Datum und Uhrzeit
- Welt
- X/Y/Z
- Rotation
- gespeichertes Inventar

Todespunkte bleiben dauerhaft erhalten.

## 13. Todesinventar

Bei einem Todespunkt kann **Inventar anzeigen** ausgewählt werden.

Angezeigt werden unter anderem:
- Inventarbereich
- Slot
- Itemname bzw. ID
- Anzahl
- Damage/Durability, sofern vorhanden

Gespeichert werden insbesondere Hauptinventar, Rüstung, Nebenhand sowie verfügbare Item-Metadaten.

Der Snapshot wird beim PlayerDeathEvent erfasst, bevor der weitere Death-Lifecycle das normale Inventar leert.

## 14. Enderchest-Verwaltung

Die Verwaltung beider Enderchest-Typen ist über:

    /verwaltung
    → Enderchests verwalten

erreichbar.

### Persönliche Enderchests

Unter **Persönliche Enderchests** werden die aktuell online befindlichen Spieler angezeigt. Nach Auswahl eines Spielers öffnet sich dessen normale **27-Slot-Enderchest** im Inventarfenster des Administrators.

Der Administrator hat vollständigen Lese- und Schreibzugriff. Items können daher:

- eingesehen
- entnommen
- hinzugefügt
- verschoben

werden.

### Team-Enderchests

Unter **Team-Enderchests** werden die vorhandenen Teams angezeigt. Nach Auswahl eines Teams öffnet sich dessen gemeinsame **54-Slot-Team-Enderchest**.

Auch hier besteht vollständiger Lese- und Schreibzugriff. Änderungen werden persistent gespeichert und gelten unmittelbar für alle Mitglieder des Teams.

Die Team-Enderchest besitzt 54 Slots und ist damit doppelt so groß wie eine normale Enderchest.


## 15. Spielerstatistiken

Eigene Statistik:

    /stats

Admin-Statistik:

    /stats admin

Die Admin-GUI zeigt:
- globale Spieleranzahl
- globale Kills
- globale Tode
- Soldaten-Kills
- Monster-Kills
- gesamte Spielzeit
- Statistiken einzelner Spieler

Spielerdetails:
- Name
- Team
- Kills
- Tode
- K/D
- Soldaten-Kills
- Monster-Kills
- Spielzeit

## 16. Gespeicherte Spielerdaten

Spielerdaten-Snapshots werden aktuell alle:

    3 Minuten

erstellt.

Zusätzlich werden Snapshots bei Join, Quit und normalem Server-Shutdown erstellt.

Gespeichert werden unter anderem:
- Inventar
- Gesundheit
- maximale Gesundheit
- Erfahrung
- Level
- Hunger
- Sättigung
- Luft
- Welt
- Position
- Blickrichtung

Bei einem abrupten Prozessabsturz oder Stromausfall kann der letzte Snapshot bis zu etwa drei Minuten alt sein.

## 17. Empfohlener Ablauf bei Spielerproblemen

1. /verwaltung öffnen.
2. Spieler auswählen.
3. Team, Position, Welt und aktiven Bann prüfen.
4. Moderationshistorie kontrollieren.
5. Bei Todes-/Inventarproblemen die Todeshistorie prüfen.
6. Bei Teamproblemen /team info <Team> verwenden.
7. Bei Wirtschaftsproblemen /eco show <Team> und /eco stats <Team> prüfen.
8. Moderationsmaßnahmen erst nach Prüfung des Sachverhalts ausführen.

## 18. Zentrale Admin-Befehle

### Verwaltung

    /verwaltung
    /verwaltung menu
    /verwaltung help

### Teams

    /team admin help
    /team admin create <Name> <Farbe>
    /team admin delete <Name>
    /team admin add <Spieler> <Team>
    /team admin remove <Spieler>
    /team admin setcolor <Name> <Farbe>

### Claims

    /claim admin help
    /claim admin set <Team>
    /claim admin delete

### Diplomatie

    /diplomatie admin help
    /diplomatie admin set <TeamA> <TeamB> <Beziehung>
    /diplomatie admin show
    /diplomatie admin show <Team>
    /diplomatie admin list

### Wirtschaft

    /eco show <Team>
    /eco stats
    /eco stats <Team>
    /eco admin set <Team> <Betrag>
    /eco admin add <Team> <Betrag>
    /eco admin remove <Team> <Betrag>

### Statistik

    /stats admin

## 19. Grundsätze

### Nachvollziehbarkeit
Administrative Änderungen sollten nachvollziehbar bleiben.

### Moderation
Bei Maßnahmen immer einen passenden Grund auswählen und die bestehende Historie berücksichtigen.

### Wirtschaft
Manuelle Änderungen am Teamkonto nur vornehmen, wenn sie tatsächlich erforderlich sind.

### Claims
Vor dem Setzen oder Löschen eines Claims immer Position und Zielteam prüfen.

### Diplomatie
Vor Änderungen beide Teamnamen und die gewünschte Beziehung kontrollieren.

### Daten
Moderations- und Todeshistorien nicht ohne konkreten Grund entfernen.

## 20. Anti-AFK

Das Anti-AFK-System überwacht die Positionsbewegung von Online-Spielern. Standardmäßig werden Spieler nach 15 Minuten Inaktivität entfernt und 60 Sekunden vorher gewarnt.

Konfiguration:

    antiafk.enabled: true
    antiafk.timeout-minutes: 15
    antiafk.warning-seconds: 60
    antiafk.exempt-permission: siedler.admin

Spieler mit `siedler.admin` sind standardmäßig ausgenommen. Die Prüfung läuft einmal pro Sekunde; reine Kopfbewegungen ohne Positionsänderung setzen den Aktivitätszeitpunkt nicht zurück.

## 21. Schnellreferenz

| Bereich | Befehl |
|---|---|
| Verwaltung | /verwaltung |
| Verwaltungshilfe | /verwaltung help |
| Teams | /team admin help |
| Claims | /claim admin help |
| Diplomatie | /diplomatie admin help |
| Wirtschaft | /eco admin ... |
| Admin-Statistik | /stats admin |
| Spieler verwalten | /verwaltung → Spieler |
| Persönliche Enderchest | /verwaltung → Enderchests verwalten → Persönliche Enderchests |
| Team-Enderchest | /verwaltung → Enderchests verwalten → Team-Enderchests |
| Moderation | /verwaltung → Spieler → Moderation |
| Moderationshistorie | /verwaltung → Spieler → Historie |
| Todeshistorie | /verwaltung → Spieler → Todeshistorie |

---

Dieses Handbuch beschreibt den aktuellen technischen Stand und sollte bei größeren Änderungen an den Siedler-Systemen aktualisiert werden.
