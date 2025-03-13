
$repoUrl = "https://github.com/joergwaldvogel/Wetterapp_funk_gommlich.git"
$repoPath = "$env:USERPROFILE\Wetterapp_funk_gommlich"


if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git ist nicht installiert, Bitte installiere Git und versuche es erneut." -ForegroundColor Red
    Read-Host "Druecke Enter, um das Skript zu beenden"
    exit 1
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


Write-Host "Baue und starte die Docker-Container..." -ForegroundColor Green
docker compose up --build -d

Write-Host "Deployment abgeschlossen, Frontend nun erreichbar unter http://localhost:5173" -ForegroundColor Green
Read-Host "Druecke Enter zum Beenden..."


