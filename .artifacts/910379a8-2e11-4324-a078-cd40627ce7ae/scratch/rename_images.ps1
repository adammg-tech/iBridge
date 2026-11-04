$files = Get-ChildItem "app/src/main/res/drawable" -Filter iPhone*.png
foreach ($file in $files) {
    $newName = $file.Name.ToLower().Replace(",", "_")
    $dest = Join-Path $file.DirectoryName $newName
    Move-Item $file.FullName $dest -Force
    Write-Host "Renamed: $($file.Name) -> $newName"
}
