$projectRoot = "$PSScriptRoot"
$webSourceDir = Join-Path $projectRoot "Web"
$nginxDir = "C:\nginx"
$nginxHtmlDir = "$nginxDir\html"

Write-Host "--- Deploying Website ---" -ForegroundColor Cyan

Write-Host "Syncing web files to Nginx..." -ForegroundColor Yellow
Copy-Item -Path "$webSourceDir\*" -Destination $nginxHtmlDir -Recurse -Force

Push-Location $nginxDir
try {
    .\nginx.exe -s reload
    Write-Host "Success! Your changes are now live." -ForegroundColor Green
} catch {
    Write-Host "Error: Nginx failed to reload." -ForegroundColor Red
}
Pop-Location