# CustomClans – Bauanleitung

Ich konnte diese `.jar` in meiner Sandbox NICHT selbst kompilieren: mein Netzwerkzugriff ist auf wenige
Paket-Registrys beschränkt (nur npm/pypi u.ä.), und die Maven-Repositories, die für ein Minecraft-Plugin
nötig sind (Maven Central, repo.papermc.io, jitpack.io), sind für mich blockiert. Deshalb bekommst du hier
den kompletten, fertigen Quellcode – du musst ihn einmal selbst mit Maven bauen. Das dauert bei
funktionierender Internetverbindung nur 1-2 Minuten.

## Variante A: Lokal auf deinem PC bauen (empfohlen)

1. **Java 17 oder neuer installieren** (falls noch nicht vorhanden): https://adoptium.net/de/temurin/releases/
2. **Maven installieren**: https://maven.apache.org/download.cgi (oder z.B. via `choco install maven` unter Windows,
   `brew install maven` unter macOS, `sudo apt install maven` unter Linux)
3. Diesen Ordner (`customclans/`) irgendwo auf deinem PC entpacken
4. Terminal/Eingabeaufforderung in diesem Ordner öffnen (dort wo `pom.xml` liegt)
5. Befehl ausführen:
   ```
   mvn clean package
   ```
6. Die fertige Datei liegt danach unter `target/CustomClans-1.0.0.jar`
7. Diese `.jar` in den `plugins/`-Ordner deines Servers hochladen (bei Nitrado per FTP)

## Variante B: Online bauen, ohne etwas lokal zu installieren

Falls du nichts installieren willst, kannst du z.B. **Gitpod** oder **GitHub Codespaces** nutzen:

1. Ein neues (privates) GitHub-Repository erstellen
2. Den Inhalt dieses Ordners hochladen
3. Codespace/Gitpod öffnen (Java + Maven sind dort meist vorinstalliert)
4. `mvn clean package` ausführen
5. Die `.jar` aus `target/` herunterladen

## Wichtig: Vault wird vorausgesetzt

Das Plugin nutzt **Vault** für die Clan-Bank (`/clan bank deposit/withdraw`). Du brauchst auf deinem Server:
- **Vault** (https://www.spigotmc.org/resources/vault.34315/)
- Ein Economy-Plugin, das Vault unterstützt, z.B. **EssentialsX**

Ohne diese beiden meldet die Konsole beim Start eine Warnung, und `/clan bank deposit`/`withdraw` sind
deaktiviert – alle anderen Befehle (`create`, `delete`, `promote`, `demote`, `kick`, `home`, `info`,
`transfer`) funktionieren trotzdem ganz normal ohne Vault.

## Alle Befehle im Überblick

| Befehl | Wer darf es | Beschreibung |
|---|---|---|
| `/clan create <name>` | jeder ohne Clan | Clan gründen, du wirst Anführer |
| `/clan delete` | Anführer | Clan auflösen |
| `/clan bank deposit <betrag>` | jedes Mitglied | Geld in die Clan-Bank einzahlen |
| `/clan bank withdraw <betrag>` | Anführer/Offizier | Geld aus der Clan-Bank abheben |
| `/clan bank balance` | jedes Mitglied | Kontostand der Clan-Bank anzeigen |
| `/clan promote <spieler>` | Anführer | Mitglied zum Offizier befördern |
| `/clan demote <spieler>` | Anführer | Offizier zum Mitglied degradieren |
| `/clan kick <spieler>` | Anführer/Offizier | Mitglied aus dem Clan werfen |
| `/clan home [name]` | jedes Mitglied | Zum Clan-Home teleportieren |
| `/clan info [name]` | jeder | Infos zu deinem oder einem anderen Clan |
| `/clan transfer <spieler>` | Anführer | Führung an ein anderes Mitglied übergeben |
| `/setclanhome [name]` | Anführer/Offizier | Clan-Home an aktueller Position setzen |

Ohne `[name]` heißt das Home bei `/clan home` und `/setclanhome` automatisch `"home"` – du kannst aber
mehrere benannte Homes anlegen, z.B. `/setclanhome basis2` und dann `/clan home basis2`.

## Datenspeicherung

Jeder Clan wird als eigene YAML-Datei unter `plugins/CustomClans/clans/<clanname>.yml` gespeichert
(Mitglieder inkl. Rang, Bank-Guthaben, Homes). Die Daten überleben Server-Neustarts automatisch.
