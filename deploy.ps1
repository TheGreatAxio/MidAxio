$sourceDir = "C:\Users\erict\WebstormProjects\MidAxio\Web"
$nginxHtmlDir = "C:\nginx\html"

Copy-Item -Path $sourceDir -Destination $nginxHtmlDir -Recurse -Force

cd C:\nginx
.\nginx.exe -s reload

Write-Host "Deployment Successful!" -ForegroundColor Green

Start-Process "https://axioscomputers.com"