# Wetterapp_funk_gommlich

# 🐋 PowerShell Deployment Script für die Wetter-App

Dieses PowerShell-Skript automatisiert das Klonen, Bauen und Starten der beiden Docker-Container für die Wetter-App. Es stellt sicher, dass alle notwendigen Abhängigkeiten installiert sind und startet die Anwendung in Containern.

## Voraussetzungen

Stelle sicher, dass folgenden Programme auf deinem System installiert sind:

1. **Docker** (inklusive `docker compose`)
    - [Download & Installation](https://www.docker.com/get-started/)
    - Führe `docker --version` aus, um zu prüfen, ob Docker korrekt installiert ist.

2. **Git**
    - [Download & Installation](https://git-scm.com/downloads)
    - Prüfe die Installation mit `git --version`.

3. **PowerShell**
   
5. **WSL-Installation**

   Falls eventuell noch nicht vorhanden, muss noch eine WSL-Installation gemacht werden. Geschieht dann einfach mit:
   
 ```powershell
wsl --install
````
    

##  Installation & Verwendung

###  **Öffne PowerShell als Administrator**
Um sicherzustellen, dass alle Befehle korrekt ausgeführt werden, öffne PowerShell mit Administratorrechten.

###  **Navigiere in den gewünschten Ordner**
Speichere das Skript (`deploy.ps1`) in einem beliebigen Verzeichnis und navigiere dorthin:

```powershell
cd C:\Pfad\zum\Ordner
````

###  **Starte das Skript mit folgendem Befehl:**

```powershell
.\deploy.ps1
````
Falls du eine Meldung erhältst, dass Skripte nicht ausgeführt werden dürfen, kannst du die Berechtigung temporär aktivieren:

```powershell
Set-ExecutionPolicy Unrestricted -Scope Process
````

##  Was macht das Skript den genau?

1. Prüft, ob Docker & Git installiert sind.

2. Falls nicht, zeigt es eine Fehlermeldung.

3. Klont das GitHub-Repo in den Benutzerordner ($HOME\Wetterapp_funk_gommlich).

4. Falls das Verzeichnis bereits existiert, wird es gelöscht und neu erstellt.

5. Baut die Docker-Container für Front- und Backend (docker compose build).

6. Startet die Container im Hintergrund (docker compose up -d). 
