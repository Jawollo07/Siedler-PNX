# Siedler 2.0 – Spieler-Handbuch

Willkommen bei **Siedler 2.0**! Dieses Handbuch erklärt die wichtigsten Funktionen für Spieler und dient als kompakter Einstieg in das Spiel.

> **Hinweis:** Das Handbuch beschreibt den aktuellen Stand des `beta`-Branches. Funktionen, die noch nicht vollständig migriert sind, werden hier nicht als verfügbar dargestellt.

---

## 1. Schnellstart

Nach dem Beitritt zum Server sind vor allem diese Befehle wichtig:

| Befehl | Funktion |
|---|---|
| `/team list` | Vorhandene Teams anzeigen |
| `/team info <Team>` | Informationen zu einem Team anzeigen |
| `/claim info` | Informationen zum Claim an deiner Position anzeigen |
| `/eco stats` | Steuerstatistiken deines Teams anzeigen |
| `/sethome <Name>` | Aktuelle Position als Home speichern |
| `/home <Name>` | Zu einem Home teleportieren |
| `/homes` | Homes als GUI anzeigen |
| `/delhome <Name>` | Ein Home löschen |
| `/tpa <Spieler>` | Teleportanfrage senden |
| `/tpaccept` | Teleportanfrage annehmen |
| `/tpdeny` | Teleportanfrage ablehnen |
| `/tpacancel` | Eigene Teleportanfrage abbrechen |
| `/death` | Zum letzten Todespunkt teleportieren |
| `/stats` | Eigene Spielerstatistik anzeigen |
| `/ec` | Eigene 27-Slot-Enderchest öffnen |
| `/tec` | Gemeinsame 54-Slot-Team-Enderchest öffnen |

---

## 2. Teams

Teams bilden die Grundlage des Siedler-Systems.

Ein Team besitzt unter anderem:

- einen Namen
- eine Farbe
- einen Kontostand
- einen Steuerbonus
- Mitglieder
- diplomatische Beziehungen
- gegebenenfalls einen Claim
- einen Eliminierungsstatus

### Teams anzeigen

```
/team list
```

Zeigt alle vorhandenen Teams.

### Team-Informationen

```
/team info <Team>
```

Zeigt Informationen über ein Team, unter anderem:

- Name
- Farbe
- Steuerbonus
- Eliminierungsstatus
- Kontostand

### Eigenes Team

Wenn du einem Team zugewiesen wurdest, kannst du dein Team über die entsprechenden Team- und Diplomatie-Befehle verwalten bzw. einsehen.

Die Erstellung, Löschung und Mitgliederverwaltung von Teams erfolgt über die Spielleitung.

---

## 3. Claims

Claims schützen Gebiete eines Teams.

### Claim anzeigen

```
/claim info
```

Der Befehl zeigt Informationen über den Claim an, in dem du dich gerade befindest.

Angezeigt werden unter anderem:

- Claim-ID
- zugehöriges Team
- Welt
- Chunk-Grenzen

### Schutz

Innerhalb geschützter Claims gelten die vom Server festgelegten Schutzregeln. Versuche daher nicht, fremde Claims zu verändern oder zu umgehen.

Die Verwaltung und Erstellung von Claims erfolgt durch die Spielleitung.

---

## 4. Wirtschaft

Die Siedler-Wirtschaft basiert auf Team-Geld.

Das Geld gehört **dem Team**, nicht einzelnen Spielern.

### Steuer

Teams erhalten Einnahmen über die Dorfbewohner-Steuer.

Die Berechnung lautet:

```
Coins = Dorfbewohner × TaxBonus × Rate
```

Der Standardwert für die Rate ist `1`.

Beispiele:

```
1 Dorfbewohner × TaxBonus 1 = 1 Coin
1 Dorfbewohner × TaxBonus 3 = 3 Coins
10 Dorfbewohner × TaxBonus 3 = 30 Coins
```

Wichtig:

- Mindestens ein Teammitglied muss online sein, damit die Steuer eingezogen wird.
- Eliminierte Teams erhalten keine Steuer.
- Die Steuer wird dem Teamkonto gutgeschrieben.
- Der Steuerzyklus läuft standardmäßig alle 24 Stunden.

### Steuerstatistiken

```
/eco stats
```

Öffnet eine GUI mit den Steuerstatistiken deines Teams.

Dort findest du unter anderem:

- bisherige Steuereinnahmen
- erfolgreiche Steuerzyklen
- fehlgeschlagene Zyklen
- insgesamt gezählte Dorfbewohner
- Informationen zur letzten Steuer

---

## 5. Diplomatie

Teams können diplomatische Beziehungen zueinander festlegen.

Es gibt drei grundlegende Beziehungen:

- **allied** – verbündet
- **neutral** – neutral
- **enemy** – feindlich

Die älteren Bezeichnungen `friendly` und `hostile` werden vom System als entsprechende Aliase behandelt.

### Beziehung setzen

Als Teammitglied:

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
```

Zeigt die Beziehungen deines Teams.

Mit einem Teamnamen kannst du gezielt dessen Beziehungen anzeigen:

```
/diplomatie show <Team>
```

---

## 6. Homes

Mit Homes kannst du wichtige Orte dauerhaft speichern.

### Home speichern

```
/sethome <Name>
```

Beispiel:

```
/sethome basis
```

Deine aktuelle Position wird unter diesem Namen gespeichert.

### Zu einem Home teleportieren

```
/home <Name>
```

Beispiel:

```
/home basis
```

### Alle Homes anzeigen

```
/homes
```

Öffnet eine GUI mit deinen gespeicherten Homes. Du kannst ein Home direkt aus der Liste auswählen.

### Home löschen

```
/delhome <Name>
```

Beispiel:

```
/delhome basis
```

Homes speichern Welt, Position und Blickrichtung und bleiben über Serverneustarts erhalten.

---

## 7. TPA – Teleportanfragen

Mit TPA kannst du andere Spieler um eine Teleportation bitten.

### Anfrage senden

```
/tpa <Spieler>
```

Beispiel:

```
/tpa Alex
```

Der Spieler erhält eine Teleportanfrage.

### Anfrage annehmen

```
/tpaccept
```

Der anfragende Spieler wird anschließend zu dir teleportiert.

### Anfrage ablehnen

```
/tpdeny
```

Lehnt die aktuelle Anfrage ab.

### Eigene Anfrage abbrechen

```
/tpacancel
```

Bricht deine eigene ausstehende TPA-Anfrage ab.

### Ablaufzeit

Eine TPA-Anfrage ist **60 Sekunden** gültig. Danach läuft sie automatisch ab.

---

## 8. Tod und Todespunkte

Nach deinem Tod wird ein Todespunkt gespeichert.

Gespeichert werden unter anderem:

- Welt
- X-, Y- und Z-Koordinaten
- Blickrichtung
- Zeitpunkt des Todes
- Inventar zum Zeitpunkt des Todes

### Zum letzten Todespunkt

```
/death
```

Teleportiert dich zu deinem zuletzt gespeicherten Todespunkt.

Der Todespunkt bleibt gespeichert und kann dadurch auch für die spätere Todeshistorie verwendet werden.

### Inventar beim Tod

Das Inventar wird beim Todesereignis **vor dem anschließenden Leeren des Inventars durch den Server** erfasst.

Gespeichert werden dabei insbesondere:

- normales Inventar
- Rüstung
- Nebenhand
- Item-ID
- Item-Name
- Anzahl
- Schaden/Durability, soweit verfügbar
- Item-Daten/NBT, soweit von der Server-API verfügbar

---

## 9. Spielerstatistiken

Mit `/stats` kannst du deine persönlichen Statistiken anzeigen.

```
/stats
```

Die Statistik enthält aktuell:

- Kills
- Tode
- K/D-Verhältnis
- Soldaten-Kills
- Monster-Kills
- Spielzeit
- Team

Die Spielzeit wird während deiner aktiven Spielsession erfasst und dauerhaft gespeichert.

---

---

## 10. Enderchests

### Persönliche Enderchest

Mit:

``
/ec
``

öffnest du deine persönliche **27-Slot-Enderchest**. Der Inhalt ist dauerhaft gespeichert und steht dir auch nach einem Serverneustart wieder zur Verfügung.

### Team-Enderchest

Mit:

``
/tec
``

öffnest du die gemeinsame **54-Slot-Team-Enderchest** deines Teams. Alle Mitglieder desselben Teams greifen auf denselben Inhalt zu.

Die Team-Enderchest hat doppelt so viele Slots wie eine normale Enderchest.

Beide Inventare können Items normal einlegen, entnehmen und verschieben. Die Inhalte werden persistent gespeichert.

---

## 11. Spielzeit und Spielerdaten

Der Server speichert regelmäßig wichtige Spielerdaten.

Dazu gehören unter anderem:

- Inventar
- Gesundheit
- maximale Gesundheit
- Erfahrung
- Level
- Hunger
- Sättigung
- Luft
- Position
- Welt
- Blickrichtung

Spielerzustände werden regelmäßig gespeichert und zusätzlich bei wichtigen Ereignissen wie Join, Quit und einem normalen Server-Shutdown erfasst.

---

## 12. Verhalten im Spiel

Siedler ist ein Team- und Strategiespiel. Für ein faires Spiel gelten daher einige Grundregeln:

### Respekt

Behandle andere Spieler respektvoll. Beleidigungen, Belästigung und absichtliches Stören anderer Spieler können Konsequenzen haben.

### Keine Cheats

Verwende keine Cheats, unerlaubten Clients oder Exploits.

### Keine Exploits

Fehler im Spiel oder Plugin sollten nicht absichtlich ausgenutzt werden. Melde kritische Fehler der Spielleitung.

### Claims respektieren

Betritt fremde Gebiete nicht mit dem Ziel, Schutzmechanismen zu umgehen oder das Gebiet unrechtmäßig zu verändern.

### Teamplay

Informationen, Ressourcen und Entscheidungen können innerhalb deines Teams entscheidend sein. Kommuniziere mit deinen Teammitgliedern und plane gemeinsam.

---

## 13. Wichtige Befehle – Übersicht

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
/diplomatie set <Team> <allied|neutral|enemy>
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

### Tod

```
/death
```

### Statistik

```
/stats
```

---

## 14. Wenn etwas nicht funktioniert

Wenn ein Befehl nicht funktioniert:

1. Prüfe die Schreibweise.
2. Prüfe, ob du die erforderliche Berechtigung besitzt.
3. Prüfe, ob du dich in einem Spieler-Kontext befindest – einige Funktionen können nicht über die Serverkonsole verwendet werden.
4. Prüfe, ob ein benötigtes Team oder Home tatsächlich existiert.
5. Bei einem technischen Fehler: Melde der Spielleitung möglichst den vollständigen Fehlertext und was du unmittelbar davor gemacht hast.

---

## 15. Kurzreferenz

| Aufgabe | Befehl |
|---|---|
| Teams ansehen | `/team list` |
| Team ansehen | `/team info <Team>` |
| Claim prüfen | `/claim info` |
| Steuerstatistik | `/eco stats` |
| Diplomatie ansehen | `/diplomatie show` |
| Diplomatie ändern | `/diplomatie set <Team> <Beziehung>` |
| Home speichern | `/sethome <Name>` |
| Home benutzen | `/home <Name>` |
| Homes anzeigen | `/homes` |
| Home löschen | `/delhome <Name>` |
| TPA senden | `/tpa <Spieler>` |
| TPA annehmen | `/tpaccept` |
| TPA ablehnen | `/tpdeny` |
| TPA abbrechen | `/tpacancel` |
| Todespunkt | `/death` |
| Eigene Statistik | `/stats` |
| Persönliche Enderchest | `/ec` |
| Team-Enderchest | `/tec` |

---

**Viel Erfolg bei Siedler 2.0 – baut euer Gebiet auf, wirtschaftet gemeinsam und behaltet eure Gegner im Blick!**
