$repoPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $repoPath

Write-Host "Baue den Docker-Container für Front- und Backend..."
docker compose build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Fehler beim Bauen des Containers!" -ForegroundColor Red
    exit 1
}

Write-Host "Starte die Docker-Container..."
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Fehler beim Starten der Container" -ForegroundColor Red
    exit 1
}

Write-Host "Laufende Container:"
docker ps

Write-Host "Deployment erfolgreich" -ForegroundColor Green
