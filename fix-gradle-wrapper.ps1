# Fix missing gradle-wrapper.jar by downloading Gradle and copying the wrapper JAR.
# Run in PowerShell: .\fix-gradle-wrapper.ps1

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
$wrapperDir = Join-Path $projectRoot "gradle\wrapper"
$gradleZipUrl = "https://services.gradle.org/distributions/gradle-8.10-bin.zip"
$tempDir = Join-Path $env:TEMP "gradle-download-$(Get-Random)"
$zipPath = Join-Path $tempDir "gradle.zip"

Write-Host "Downloading Gradle (this may take a minute)..."
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
Invoke-WebRequest -Uri $gradleZipUrl -OutFile $zipPath -UseBasicParsing

Write-Host "Extracting..."
Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force
$extractedName = (Get-ChildItem $tempDir -Directory | Where-Object { $_.Name -like "gradle-*" } | Select-Object -First 1).Name
$libDir = Join-Path $tempDir "$extractedName\lib"
$wrapperJar = Get-ChildItem -Path $libDir -Filter "gradle-wrapper*.jar" | Select-Object -First 1
if (-not $wrapperJar) {
    Write-Host "ERROR: gradle-wrapper jar not found in $libDir"
    exit 1
}

$destPath = Join-Path $wrapperDir "gradle-wrapper.jar"
Copy-Item -Path $wrapperJar.FullName -Destination $destPath -Force
Write-Host "Copied to $destPath"

Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "Done. You can now run: gradlew.bat assembleDebug"
Write-Host ""
