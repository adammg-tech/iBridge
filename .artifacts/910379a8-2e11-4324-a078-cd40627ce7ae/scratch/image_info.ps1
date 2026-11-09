Add-Type -AssemblyName System.Drawing
$iconPath = (Get-Item "app/src/main/ic_launcher-playstore.png").FullName
$bannerPath = (Get-Item "Banner.png").FullName

$icon = [System.Drawing.Image]::FromFile($iconPath)
$banner = [System.Drawing.Image]::FromFile($bannerPath)

Write-Host "Icon: $($icon.Width) x $($icon.Height) pixels ($([Math]::Round((Get-Item $iconPath).Length / 1KB, 2)) KB)"
Write-Host "Banner: $($banner.Width) x $($banner.Height) pixels ($([Math]::Round((Get-Item $bannerPath).Length / 1KB, 2)) KB)"
