
$repoUrl = "https://github.com/joergwaldvogel/Wetterapp_funk_gommlich.git"
$repoPath = "$env:USERPROFILE\Wetterapp_funk_gommlich"


if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git ist nicht installiert, Bitte installiere Git und versuche es erneut." -ForegroundColor Red
}

if (Test-Path $repoPath) {
    Write-Host "Repository existiert bereits. Pulling latest changes..." -ForegroundColor Cyan
    Set-Location $repoPath
    git pull
} else {
    Write-Host "Klone Repository nach $repoPath ..." -ForegroundColor Green
    git clone $repoUrl $repoPath
    Set-Location $repoPath
}

if (-not (docker info | Out-Null)) {
    Write-Host "Docker-Daemon läuft nicht. Bitte starte Docker und versuche es erneut." -ForegroundColor Red
}

Write-Host "Baue und starte die Docker-Container..." -ForegroundColor Green
docker compose up --build -d

Write-Host "Deployment abgeschlossen, Frontend nun erreichbar unter http://localhost:5173" -ForegroundColor Green
