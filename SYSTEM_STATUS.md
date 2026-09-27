# Siedler-PNX – System Status & Test Checklist

> **Branch:** beta  
> **Purpose:** Praktische Verifikations-Checkliste für Siedler 2.0.  
> **Wichtig:** Ein System wird erst als **funktionierend** markiert, wenn es auf dem Ziel-PowerNukkitX-Server getestet wurde.

## Status-Legende

- [ ] **Nicht getestet**
- [x] **Verifiziert funktionierend**
- [~] **Teilweise funktionierend**
- [!] **Bekannter Fehler**
- [N/A] **Nicht zutreffend**

## 1. Core / Server-Lifecycle

| Status | System | Prüfung |
|---|---|---|
| [x] | Plugin lädt | Server startet ohne Siedler-Fehler |
| [x] | Plugin deaktivieren | Server stoppt sauber |
| [x] | Manager-Initialisierung | Alle Manager starten |
| [x] | Konfiguration | config.yml wird korrekt geladen |
| [x] | MessageManager | Spieler-Meldungen kommen aus messages.yml |
| [x] | Tree Command API | Commands registrieren/routen korrekt |
| [x] | Permission-System | siedler.admin schützt Admin-Routen |
| [x] | Join | Spielerdaten werden erstellt/aktualisiert |
| [x] | Quit | Spielerdaten werden gespeichert |
| [x] | Restart-Persistenz | Daten bleiben nach Neustart erhalten |

## 2. Storage / Datenbank

| Status | System | Prüfung |
|---|---|---|
| [x] | SQLite | Neue DB initialisiert |
| [x] | SQLite-Persistenz | Daten überleben Neustart |
| [x] | MariaDB-Verbindung | Verbindung funktioniert |
| [x] | MariaDB-Schema | Alle Tabellen/Spalten vorhanden |
| [x] | Schema-Versionierung | Bestehende DB wird korrekt migriert |
| [x] | Spieler-IDs | Stabile IDs/UUIDs werden verwendet |
| [x] | Transaktionen | Kritische DB-Vorgänge sind atomar |
| [x] | Fehlerbehandlung | SQL-Fehler crashen nicht das Gameplay |

**Bekannter Prüfpunkt:** Bei alten MariaDB-Datenbanken kann eine Migration für players.id erforderlich sein.

## 3. Teams

| Status | System | Prüfung |
|---|---|---|
| [x] | Team erstellen | Team kann erstellt werden |
| [x] | Team löschen | Team kann sicher gelöscht werden |
| [x] | Mitglieder | Spieler hinzufügen/entfernen |
| [x] | Stabile Spieleridentität | Rename/Rejoin erzeugt keine Duplikate |
| [x] | Teamfarben | Farben/Anzeige funktionieren |
| [x] | Teaminfo | Info/List funktionieren |
| [x] | Teamchat | Team-Nachrichten funktionieren |
| [x] | Persistenz | Teams überleben Neustart |

## 4. Diplomatie / Relations

| Status | System | Prüfung |
|---|---|---|
| [ ] | Relation setzen | Beziehung zwischen Teams setzen |
| [ ] | Neutral | Neutraler Zustand funktioniert |
| [ ] | Allianz | Allianz funktioniert |
| [ ] | Anzeigen | Diplomatie-Anzeige funktioniert |
| [ ] | Admin-Verwaltung | Admin-Routen funktionieren |
| [ ] | Persistenz | Relations überleben Neustart |

## 5. Claims

| Status | System | Prüfung |
|---|---|---|
| [x] | Claim erstellen | Team kann Claim erstellen |
| [x] | Claim löschen | Berechtigtes Team kann Claim löschen |
| [x] | Claim-Info | Richtiger Claim wird erkannt |
| [x] | Welten | Claims sind nach Welt getrennt |
| [x] | Grenzen | Chunk-/Blockgrenzen stimmen |
| [x] | Block-Abbau | Fremde Spieler können nicht abbauen |
| [x] | Block-Platzieren | Fremde Spieler können nicht bauen |
| [x] | Interaktionen | Buttons/Lever/etc. sind geschützt |
| [x] | Teammitglieder | Mitglieder können normal bauen |
| [x] | Admin-Funktionen | Admin-Funktionen funktionieren |
| [x] | Grenzvisualisierung | Nahe Claim-Grenzen sind dauerhaft sichtbar |
| [x] | TNT/Explosionen | Explosions-Griefing ist verhindert |
| [x] | Pistons | Pistons umgehen Claims nicht |
| [ ] | Feuer | Feuer umgeht Claims nicht | Schutz für Burn/Ignite ergänzt; Ingame verifizieren
| [ ] | Flüssigkeiten | Wasser/Lava umgehen Claims nicht | BlockFromTo-Schutz ergänzt; Ingame verifizieren
| [x] | Container/Hopper | Geschützte Inventare können nicht missbraucht werden |
| [x] | Entity-Griefing | Relevante Entity-Blockänderungen sind geschützt |
| [x] | Protection-Schalter | claims.protection.enabled funktioniert |
| [x] | Persistenz | Claims überleben Neustart |

## 6. Economy

| Status | System | Prüfung |
|---|---|---|
| [x] | Team-Guthaben | Balance existiert/persistiert |
| [x] | Geld hinzufügen | Admin kann Geld hinzufügen |
| [x] | Geld entfernen | Admin kann Geld entfernen |
| [x] | Balance setzen | Admin kann Balance setzen |
| [x] | Balance anzeigen | Spieler können Balance sehen |
| [x] | Transaktionen | Transaktionen werden protokolliert |
| [x] | Balance-History | Historie wird gespeichert |
| [x] | Atomare Zahlung | Fehlgeschlagene Zahlung rollt sauber zurück |
| [x] | Persistenz | Guthaben überlebt Neustart |

## 7. Steuern / TaxBonus

| Status | System | Prüfung |
|---|---|---|
| [ ] | Villager-Zählung | Richtige Villager werden gezählt |
| [ ] | Steuerformel | Villager × TaxBonus × Rate stimmt |
| [ ] | TaxBonus | Team-TaxBonus wird angewendet |
| [ ] | Online-Mitglied | Steuer nur bei mindestens einem Online-Mitglied |
| [ ] | Eliminierte Teams | Eliminierte Teams erhalten keine Steuer |
| [ ] | Steuerzyklus | Automatische Sammlung funktioniert |
| [ ] | Fehlerprotokoll | Fehlgeschlagene Sammlung wird gespeichert |
| [ ] | Retry | Wiederholung funktioniert |
| [ ] | Statistik-UI | eco stats funktioniert |
| [ ] | Team-Statistik | Auswahl eines Teams funktioniert |
| [ ] | Persistenz | Steuerdaten überleben Neustart |

## 8. Eliminierung / Spectator

| Status | System | Prüfung |
|---|---|---|
| [x] | Team eliminieren | Admin kann Team eliminieren |
| [x] | De-Eliminieren | Admin kann Team wiederherstellen |
| [x] | Eliminierungsstatus | Status wird gespeichert |
| [x] | Permanenter Spectator | Eliminierte Spieler werden korrekt behandelt |
| [ ] | Eliminierungsblock | Konfigurierter Block funktioniert |
| [x] | Claim-Verhalten | Eliminierte Teams umgehen Schutz nicht |
| [x] | Persistenz | Status überlebt Neustart |

## 9. Homes

| Status | System | Prüfung |
|---|---|---|
| [x] | sethome | Home wird gespeichert |
| [x] | home | Teleport funktioniert |
| [x] | homes | GUI funktioniert |
| [x] | delhome | Home wird gelöscht |
| [x] | Welt | Welt wird korrekt wiederhergestellt |
| [x] | Position | Koordinaten stimmen |
| [x] | Rotation | Yaw/Pitch stimmen |
| [x] | Persistenz | Homes überleben Neustart |

## 10. TPA

| Status | System | Prüfung |
|---|---|---|
| [ ] | tpa | Anfrage kann gesendet werden |
| [ ] | tpaccept | Anfrage wird angenommen |
| [ ] | tpdeny | Anfrage wird abgelehnt |
| [ ] | tpacancel | Anfrage kann abgebrochen werden |
| [ ] | Timeout | Abgelaufene Anfrage nicht mehr annehmbar |
| [x] | Offline-Spieler | Offline-Ziele werden sicher behandelt |

## 11. Essentials / Administration

| Status | System | Prüfung |
|---|---|---|
| [!] | Verwaltung-GUI | Admin-GUI öffnet | Notes: "\n" Wird als Text behandelt
| [x] | Spieler-Lookup | Spielerinformationen werden angezeigt |
| [x] | Kick | Kick funktioniert |
| [ ] | Warnung | Warnung funktioniert | Warnung wird nun zusätzlich direkt an den Spieler gesendet; Ingame verifizieren
| [x] | Ban | Permanenter Ban funktioniert |
| [x] | Tempban | Temporärer Ban läuft korrekt ab |
| [x] | Join-Ban | Gebannte Spieler können nicht joinen |
| [x] | Moderations-History | Aktionen werden gespeichert |
| [x] | Death-History | Todesdaten werden gespeichert |
| [ ] | Inventory-Snapshots | Snapshots werden gespeichert | Join/Quit/periodische Snapshots und Admin-Anzeige vorhanden; Ingame/DB verifizieren
| [ ] | Snapshot-Verwaltung | Admin kann Snapshots ansehen/verwalten | Verwaltung um Snapshot-Ansicht erweitert; Ingame verifizieren
| [x] | Permissions | Unberechtigte Spieler kommen nicht hinein |

## 12. Enderchests / Inventare

| Status | System | Prüfung |
|---|---|---|
| [ ] | ec | Persönlicher Enderchest öffnet | Fehlgeschlagenes addWindow setzt EnderChest-Status zurück; Ingame verifizieren
| [x] | Personal-Persistenz | Inhalt überlebt Neustart |
| [ ] | tec | Team-Enderchest öffnet | Slot-Mapping an native ChestInventory angepasst; Ingame verifizieren
| [x] | Team-Persistenz | Inhalt überlebt Neustart |
| [x] | Teamzugriff | Nur berechtigte Mitglieder greifen zu |
| [x] | Verwaltung | Admin kann Enderchests verwalten |
| [x] | MariaDB | Inventare funktionieren mit MariaDB |

## 13. Statistiken

| Status | System | Prüfung |
|---|---|---|
| [!] | Spielerstatistiken | Werte werden aufgezeichnet | Notes: "\n" wird als Text erkannt
| [!] | Anzeige | Statistiken können angezeigt werden | Notes: "\n" wird als Text erkannt
| [x] | Persistenz | Statistiken überleben Neustart |
| [x] | Tode | Tode werden aufgezeichnet |
| [x] | Kills | Kills werden aufgezeichnet |

## 14. Anti-AFK

| Status | System | Prüfung |
|---|---|---|
| [ ] | AFK-Erkennung | Inaktivität wird erkannt |
| [ ] | Warnung | Warnung wird gesendet |
| [ ] | Auto-Kick | Kick nach Timeout |
| [ ] | Konfiguration | Zeiten sind konfigurierbar |
| [ ] | False Positives | Aktive Spieler werden nicht fälschlich gekickt |

## 15. Monster-Control

| Status | System | Prüfung |
|---|---|---|
| [ ] | Global Controller | Normales Monster-Spawning wird kontrolliert |
| [ ] | Blacklist | Blacklisted Mobs werden blockiert |
| [ ] | Controlled-Mob-Liste | Verwaltete Mobs korrekt behandelt |
| [ ] | Quotas | Spawn-Wahrscheinlichkeiten funktionieren |
| [ ] | Chunk-Limit | Chunk-Limit funktioniert |
| [ ] | World-Limit | World-Limit funktioniert |
| [ ] | World-Blacklist | Konfigurierte Welten geschützt |
| [ ] | Claim-Schutz | Normale Monster in Claims blockiert |
| [ ] | Villager-Limit | Lokales Villager-Limit funktioniert |
| [ ] | Token-Bypass | Token-Encounter können spawnen |
| [ ] | Raid-Bypass | Raid-Encounter können spawnen |

## 16. Tokens

| Status | System | Prüfung |
|---|---|---|
| [ ] | Token-Runden | Runden werden gespeichert |
| [ ] | Spawning | Tokens spawnen korrekt |
| [ ] | Aktiv-Limit | Limit funktioniert |
| [ ] | Sicherer Spawn | Keine ungültigen Spawnpositionen |
| [ ] | Token defeat | Niederlage wird erkannt |
| [ ] | TaxBonus | Team erhält dauerhaften TaxBonus |
| [ ] | Admin start | token admin start funktioniert |
| [ ] | Admin spawn | token admin spawn funktioniert |
| [ ] | Admin status | token admin status funktioniert |
| [ ] | Restart | Token-Zustand wird sicher behandelt |

## 17. Outposts

| Status | System | Prüfung |
|---|---|---|
| [ ] | Outpost | Capture-Point funktioniert |
| [ ] | Radius | Radius wird eingehalten |
| [ ] | Timer | Capture-Zeit funktioniert |
| [ ] | Besitz | Richtiger Besitzer wird gesetzt |
| [ ] | TaxBonus | Belohnung wird vergeben |
| [ ] | Persistenz | Besitz überlebt Neustart |
| [ ] | Admin-Verwaltung | Admin-Funktionen funktionieren |

## 18. Raids / Pillager

| Status | System | Prüfung |
|---|---|---|
| [ ] | Raid starten | Raid kann gestartet werden |
| [ ] | Wellen | Mehrere Wellen funktionieren |
| [ ] | Squad-Größe | Konfiguration wird beachtet |
| [ ] | Spawnradius | Spawnradius stimmt |
| [ ] | Pillager | Pillager funktionieren |
| [ ] | Vindicator | Vindicator funktionieren |
| [ ] | Ravager | Option funktioniert |
| [ ] | Outpost-Zuordnung | Richtiger Outpost wird angegriffen |
| [ ] | Raid status | Status funktioniert |
| [ ] | Admin start/stop | Admin-Befehle funktionieren |
| [ ] | Restart | Aktive Raids werden sicher beendet |

## 19. Market / Trader

| Status | System | Prüfung |
|---|---|---|
| [ ] | Marktbereich | Bereich funktioniert |
| [ ] | Marktschutz | Blockschutz funktioniert |
| [ ] | Monsterfrei | Normale Monster werden blockiert/entfernt |
| [ ] | Trader Entities | VillagerV2-Trader funktionieren |
| [ ] | Trader UI | Handelsmenü öffnet |
| [ ] | Team-Geld | Kauf belastet Teamkonto |
| [ ] | Atomare Zahlung | Fehlgeschlagener Kauf verliert kein Geld |
| [ ] | Material-Anforderungen | Zusätzliche Anforderungen funktionieren |
| [ ] | Custom Trades | Konfigurierbare Trades funktionieren |
| [ ] | Siedler-3 Presets | Presets funktionieren |
| [ ] | Market-Befehle | Player-Routen funktionieren |
| [ ] | Market-Admin | reload/cleanup/spawn funktionieren |

## 20. Kommunikation

| Status | System | Prüfung |
|---|---|---|
| [ ] | Global Chat | Öffentlicher Chat funktioniert |
| [ ] | Team Chat | Teamchat funktioniert |
| [ ] | Direct Message | dm funktioniert |
| [ ] | MessageManager | Meldungen sind zentralisiert |
| [ ] | Prefixe | Richtige Prefixe |
| [ ] | messages.yml | Änderungen werden übernommen |

## 21. PvP / Tournament

| Status | System | Prüfung |
|---|---|---|
| [ ] | PvP-Arena | Arena funktioniert |
| [ ] | Mehrere Arenen | Arenen können parallel existieren |
| [ ] | Mehrere Matches | Matches laufen unabhängig |
| [ ] | PvP-Kits | Kits sind konfigurierbar |
| [ ] | Kit-Auswahl | Spieler können Kits wählen |
| [ ] | Match-Lifecycle | Start/Kampf/Ende funktioniert |
| [ ] | KO-Modus | Knockout-Modus funktioniert |
| [ ] | Double Elimination | Winners-/Losers-Bracket funktioniert |
| [ ] | Grand Final | Grand Final funktioniert |
| [ ] | Match-Isolation | Matches beeinflussen sich nicht |
| [ ] | Spieler-Restoration | Inventar/Position/State werden wiederhergestellt |
| [ ] | Arena-Reset | Arena wird zurückgesetzt |
| [ ] | Spectator | Zuschauer können nicht eingreifen |
| [ ] | Restart | Matchzustände werden sicher behandelt |

## 22. Minefield / Control

| Status | System | Prüfung |
|---|---|---|
| [ ] | Minefield | Minefield kann erstellt werden |
| [ ] | Minen | Minen können gesetzt werden |
| [ ] | Detektion | Minen lösen korrekt aus |
| [ ] | Team-Besitz | Besitz wird beachtet |
| [ ] | Damage/Effekte | Effekte funktionieren |
| [ ] | Control Points | Kontrollsystem funktioniert |
| [ ] | Persistenz | Zustand überlebt Neustart |

## 23. Soldiers

| Status | System | Prüfung |
|---|---|---|
| [ ] | Soldier Entity | Basis-Entity funktioniert |
| [ ] | Infantry | Infanterie funktioniert |
| [ ] | Archer | Bogenschütze funktioniert |
| [ ] | Cavalry | Kavallerie funktioniert |
| [ ] | Equipment | Waffen/Rüstung werden korrekt dargestellt |
| [ ] | Spawning | Soldaten können gespawnt werden |
| [ ] | Ownership | Richtige Teamzugehörigkeit |
| [ ] | AI | KI funktioniert |
| [ ] | Groups | Gruppenverwaltung funktioniert |
| [ ] | Levels | Levelsystem funktioniert |
| [ ] | Combat | Kampf funktioniert |
| [ ] | Persistenz | Soldaten können sicher gespeichert/geladen werden |

## 24. Cross-System / Regression

| Status | Szenario | Prüfung |
|---|---|---|
| [ ] | Fresh Server | Start mit leerer DB |
| [ ] | Bestehende SQLite | Alte Daten bleiben nutzbar |
| [ ] | Bestehende MariaDB | Alte Daten bleiben nutzbar |
| [ ] | Server Restart | Alle persistenten Systeme laden |
| [ ] | Join/Quit | Keine Datenverluste |
| [ ] | Team + Claim | Mitglieder können Claim benutzen |
| [ ] | Claim + Economy | Claim gehört zum richtigen Team |
| [ ] | Elimination + Claim | Eliminierung wird korrekt behandelt |
| [ ] | Elimination + Spectator | Spectator kann nicht umgehen |
| [ ] | Tax + Villager | Steuer stimmt mit Villagerzahl |
| [ ] | Tax + Online | Offline-Teams erhalten keine Steuer |
| [ ] | Token + TaxBonus | Token-Bonus wirkt auf zukünftige Steuern |
| [ ] | Outpost + TaxBonus | Outpost-Bonus wirkt |
| [ ] | Outpost + Raid | Raid trifft richtigen Besitzer |
| [ ] | Market + Economy | Kauf belastet korrekt |
| [ ] | Moderation + Join | Ban funktioniert nach Restart |
| [ ] | Inventory + Restart | Inhalte bleiben erhalten |
| [ ] | Home + Restart | Home bleibt gültig |
| [ ] | TPA + Disconnect | Requests werden bereinigt |
| [ ] | Anti-AFK + Combat | Aktive Spieler werden nicht gekickt |
| [ ] | Monster + Token/Raid | Spezial-Encounters werden nicht blockiert |
| [ ] | Multiple Matches | Match-Zustände bleiben getrennt |

## 25. Bekannte Fehler / offene Prüfungen

| Status | Problem | Notiz |
|---|---|---|
| [ ] | MariaDB players.id Migration | Alte Schemas ohne players.id können SQL-Fehler verursachen |
| [ ] | Claim Protection Regression | Fire/Ignite/Liquid/Explosion-Schutz erweitert; vollständigen Ingame-Test durchführen |
| [ ] | /ec Runtime-Test | Fehlgeschlagenes Öffnen wird sauber zurückgesetzt; Öffnen/Persistenz testen |
| [ ] | /home Runtime-Test | Location-Konstruktor wurde auf double yaw/pitch korrigiert; Ingame testen |

## 26. Release Gate

Vor dem vollständigen Beta-Abschluss müssen mindestens diese Bereiche verifiziert sein:

- [ ] Core / Lifecycle
- [ ] Datenbank
- [ ] Teams
- [ ] Claims + Protection
- [ ] Economy
- [ ] Taxes
- [ ] Elimination / Spectator
- [ ] Essentials / Moderation
- [ ] Inventare
- [ ] Homes / TPA
- [ ] Monster-Control
- [ ] Tokens
- [ ] Outposts / Raids
- [ ] Market / Trader
- [ ] Kommunikation
- [ ] PvP / Tournament
- [ ] Minefield
- [ ] Soldiers
- [ ] Cross-System Regression

## Testprotokoll

**Letzter vollständiger Test:** __________________

**Minecraft/Bedrock-Version:** __________________

**PowerNukkitX-Version:** __________________

**Datenbank:** SQLite / MariaDB

**Tester:** __________________

**Notizen:**

____________________________________________________________

____________________________________________________________

____________________________________________________________
