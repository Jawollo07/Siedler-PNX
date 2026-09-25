# Siedler 2.0 – Spieler-Handbuch

Willkommen bei **Siedler 2.0**! Dieses Handbuch beschreibt die aktuell für Spieler relevanten Funktionen des PowerNukkitX-Ports.

> **Stand:** aktueller `beta`-Branch von Siedler-PNX. Administrative und noch nicht migrierte Funktionen sind nicht als Spielerfunktionen dokumentiert.

## 1. Schnellstart

| Befehl | Funktion |
|---|---|
| `/team list` | Teams anzeigen |
| `/team info <Team>` | Teaminformationen anzeigen |
| `/claim info` | Claim an der aktuellen Position prüfen |
| `/eco stats` | Steuerstatistik des eigenen Teams |
| `/sethome <Name>` | Home speichern |
| `/home <Name>` | Home benutzen |
| `/homes` | Homes anzeigen |
| `/delhome <Name>` | Home löschen |
| `/tpa <Spieler>` | Teleportanfrage senden |
| `/tpaccept` | Teleportanfrage annehmen |
| `/tpdeny` | Teleportanfrage ablehnen |
| `/tpacancel` | Eigene Teleportanfrage abbrechen |
| `/death` | Zum letzten Todespunkt |
| `/stats` | Eigene Statistik |
| `/ec` | Persönliche Enderchest |
| `/tec` | Gemeinsame Team-Enderchest |
| `/outpost list` | Outposts anzeigen |
| `/outpost info <Name>` | Outpost-Informationen |
| `/raid status` | Laufenden Raid anzeigen |
| `/market info` | Marktplatz an der aktuellen Position |
| `/market types` | Händlerarten anzeigen |

## 2. Teams

Teams sind die Grundlage des Siedler-Systems. Ein Team besitzt unter anderem einen Namen, eine Farbe, Mitglieder, ein Konto, einen TaxBonus, diplomatische Beziehungen und gegebenenfalls Claims oder Outposts.

### Teams anzeigen

```
/team list
```

### Teaminformationen

```
/team info <Team>
```

Dort werden unter anderem Name, Farbe, TaxBonus, Eliminierungsstatus und Kontostand angezeigt.

Die Erstellung, Löschung und Mitgliederverwaltung erfolgt durch die Spielleitung.

## 3. Claims

Claims schützen Gebiete eines Teams.

### Claim prüfen

```
/claim info
```

Angezeigt werden unter anderem Claim-ID, Team, Welt und Chunk-Grenzen.

Innerhalb eines Siedler-Claims gelten die serverseitigen Schutzregeln. Normale feindliche Monster spawnen dort nicht.

## 4. Wirtschaft und Steuern

Die Siedler-Wirtschaft basiert auf Team-Geld. Das Guthaben gehört dem Team und nicht einzelnen Spielern.

Die Steuerberechnung lautet:

```
Coins = Dorfbewohner × TaxBonus × Rate
```

Bei der Standardrate von 1 gilt beispielsweise:

```
1 Dorfbewohner × TaxBonus 1 = 1 Coin
1 Dorfbewohner × TaxBonus 3 = 3 Coins
10 Dorfbewohner × TaxBonus 3 = 30 Coins
```

Wichtig:
- Mindestens ein Teammitglied muss online sein.
- Eliminierte Teams erhalten keine Steuer.
- Die Einnahme wird dem Teamkonto gutgeschrieben.
- Der Standard-Steuerzyklus läuft alle 24 Stunden.

### Steuerstatistik

```
/eco stats
```

Die GUI zeigt unter anderem Einnahmen, erfolgreiche und fehlgeschlagene Steuerzyklen, gezählte Dorfbewohner und Informationen zur letzten Steuer.

## 5. Diplomatie

Teams können Beziehungen zueinander verwalten:

- `allied` – verbündet
- `neutral` – neutral
- `enemy` – feindlich

### Beziehung setzen

```
/diplomatie set <Team> <Beziehung>
```

Beispiel:

```
/diplomatie set Blau allied
```

### Beziehungen anzeigen

```
/diplomatie show
/diplomatie show <Team>
```

## 6. Homes

```
/sethome <Name>
/home <Name>
/homes
/delhome <Name>
```

Homes speichern Welt, Position und Blickrichtung und bleiben über Serverneustarts erhalten.

## 7. TPA

Mit TPA können Spieler Teleportanfragen senden:

```
/tpa <Spieler>
/tpaccept
/tpdeny
/tpacancel
```

Eine Anfrage ist standardmäßig 60 Sekunden gültig.

## 8. Tod und Todespunkte

Nach einem Tod wird ein Todespunkt gespeichert. Dazu gehören insbesondere Welt, Position, Rotation und Zeitpunkt.

```
/death
```

teleportiert zum letzten gespeicherten Todespunkt.

Beim Tod wird außerdem ein Inventar-Snapshot erfasst, soweit die verfügbaren PNX-Itemdaten dies erlauben. Dazu gehören Hauptinventar, Rüstung und Nebenhand.

## 9. Spielerstatistiken

```
/stats
```

Die Statistik enthält aktuell unter anderem:

- Kills
- Tode
- K/D-Verhältnis
- Soldaten-Kills
- Monster-Kills
- Spielzeit
- Team

## 10. Enderchests

### Persönliche Enderchest

```
/ec
```

Öffnet die persönliche **27-Slot-Enderchest**. Der Inhalt wird dauerhaft gespeichert.

### Team-Enderchest

```
/tec
```

Öffnet die gemeinsame **54-Slot-Team-Enderchest**. Alle Mitglieder desselben Teams greifen auf denselben Inhalt zu.

## 11. Outposts

Outposts sind besondere Kontrollpunkte, die von Teams erobert werden können.

### Anzeigen

```
/outpost list
/outpost info <Name>
```

Ein Outpost zeigt unter anderem Welt, Position, Radius, Besitzer und Capture-Fortschritt.

### Eroberung

Befindet sich ein berechtigtes Team innerhalb des Capture-Bereichs, wird der Outpost erobert. Sind gegnerische Teams gleichzeitig anwesend, ist der Outpost umkämpft und der Fortschritt pausiert.

Ein eroberter Outpost gewährt seinem Besitzer einen dauerhaften TaxBonus entsprechend der Serverkonfiguration.

## 12. Pillager-Raids

Besetzte Outposts können von Pillager-Squads angegriffen werden. Ein Raid besteht aus mehreren Wellen.

```
/raid status
```

zeigt den aktuell laufenden Raid und die aktuelle Welle.

Je nach Konfiguration können Pillager, Vindicator und Ravager auftreten. Der Raid endet nach erfolgreicher Verteidigung aller Wellen.

## 13. Monstersteuerung

Normale feindliche Monster werden zentral vom Server gesteuert.

Für Spieler bedeutet das insbesondere:

- In Siedler-Claims spawnen keine normalen feindlichen Monster.
- Der Server kann einzelne Monster vollständig blockieren.
- Spawnwahrscheinlichkeiten und Monsterlimits können serverseitig angepasst werden.
- Bestimmte Welten können von normalen Monster-Spawns ausgeschlossen werden.

Token-Monster und Pillager-Raids sind davon getrennt. Diese werden vom Siedler-System gezielt erzeugt.

## 14. Marktplatz

Marktplätze sind geschützte Handelsbereiche.

Innerhalb eines konfigurierten Marktes können Spieler normalerweise weder Blöcke abbauen noch platzieren. Bestimmte Interaktionen können ebenfalls deaktiviert sein. Normale feindliche Monster werden dort nicht zugelassen.

### Marktplatz prüfen

```
/market info
```

### Händlerarten

```
/market types
```

zeigt die auf dem Server konfigurierten Händlerarten.

### Händler

Siedler verwendet native PowerNukkitX-Händler mit dem normalen Bedrock-Handelsfenster. Welche Angebote verfügbar sind, wird serverseitig konfiguriert.

Die Standardkonfiguration enthält unter anderem Händler für:

- Lebensmittel
- Baustoffe
- Rohstoffe
- Werkzeuge
- Waffen
- Versorgung
- Soldaten
- Verzauberungen

Die verwendete Währung ist standardmäßig Emerald, sofern die Serverkonfiguration nicht geändert wurde.

## 15. Anti-AFK

Das Anti-AFK-System erkennt längere Inaktivität anhand tatsächlicher Positionsbewegung.

Standardmäßig:

- Warnung 60 Sekunden vor Ablauf
- Entfernung nach 15 Minuten ohne relevante Bewegung
- reine Kopfbewegungen zählen nicht als Aktivität
- Spieler mit der konfigurierten Ausnahmeberechtigung sind ausgenommen

## 16. Gespeicherte Spielerdaten

Wichtige Spielerdaten werden persistent gespeichert, darunter je nach System:

- Inventar
- Gesundheit
- Erfahrung und Level
- Hunger und Sättigung
- Position und Welt
- Blickrichtung
- Homes
- Statistiken
- Todespunkte

## 17. Wichtige Befehle

### Teams

```
/team help
/team list
/team info <Team>
```

### Claims

```
/claim help
/claim info
```

### Wirtschaft

```
/eco help
/eco stats
/eco stats <Team>
```

### Diplomatie

```
/diplomatie help
/diplomatie set <Team> <Beziehung>
/diplomatie show
/diplomatie show <Team>
```

### Homes

```
/sethome <Name>
/home <Name>
/homes
/delhome <Name>
```

### Teleport

```
/tpa <Spieler>
/tpaccept
/tpdeny
/tpacancel
```

### Monster-/Outpost-System

```
/outpost list
/outpost info <Name>
/raid status
```

### Markt

```
/market help
/market info
/market types
```

### Sonstiges

```
/death
/stats
/ec
/tec
```

## 18. Wenn etwas nicht funktioniert

1. Schreibweise des Befehls prüfen.
2. Prüfen, ob der Befehl nur im Spiel und nicht über die Konsole funktioniert.
3. Prüfen, ob das benötigte Team, Home oder Ziel existiert.
4. Bei einem technischen Fehler den vollständigen Fehlertext an die Spielleitung weitergeben.

## 19. Verhalten im Spiel

Für ein faires Siedler-Spiel gelten insbesondere:

- andere Spieler respektieren,
- keine Cheats oder unerlaubten Clients verwenden,
- keine Exploits absichtlich ausnutzen,
- fremde Claims respektieren,
- Teamplay und Kommunikation nutzen.

---

**Viel Erfolg bei Siedler 2.0 – baut euer Gebiet auf, entwickelt eure Wirtschaft und verteidigt eure Outposts!**
