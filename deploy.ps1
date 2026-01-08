$projectRoot = "$PSScriptRoot"
$sourceDir = Join-Path $projectRoot "Web"
$nginxHtmlDir = "C:\nginx\html"

Write-Host "Syncing files from: $sourceDir" -ForegroundColor Cyan

Copy-Item -Path "$sourceDir\*" -Destination $nginxHtmlDir -Recurse -Force

Push-Location C:\nginx
try {
    .\nginx.exe -s reload
    Write-Host "Deployment Successful! Nginx reloaded." -ForegroundColor Green
} catch {
    Write-Host "Failed to reload Nginx. Is it running?" -ForegroundColor Red
}
Pop-Location