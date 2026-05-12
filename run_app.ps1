# EcoTrack Enterprise: Emulator & Deployment Script

# 1. Start the emulator in the background
Write-Host "🚀 Launching Pixel_8a Emulator..." -ForegroundColor Cyan
Start-Process -FilePath "$env:LOCALAPPDATA\Android\sdk\emulator\emulator.exe" -ArgumentList "-avd Pixel_8a" -WindowStyle Minimized

# 2. Wait for the emulator to be ready
Write-Host "⏳ Waiting for device to boot..." -ForegroundColor Yellow
$booted = $false
while (-not $booted) {
    $status = & "$env:LOCALAPPDATA\Android\sdk\platform-tools\adb.exe" shell getprop sys.boot_completed
    if ($status -eq "1") {
        $booted = $true
    } else {
        Start-Sleep -Seconds 5
    }
}

Write-Host "✅ Emulator is ready!" -ForegroundColor Green

# 3. Build and Install the APK
Write-Host "📦 Building and Installing EcoTrack Enterprise..." -ForegroundColor Cyan
./gradlew.bat installDebug

# 4. Launch the App
Write-Host "🎯 Launching EcoTrack..." -ForegroundColor Green
& "$env:LOCALAPPDATA\Android\sdk\platform-tools\adb.exe" shell am start -n com.ecotrack.enterprise/com.ecotrack.enterprise.presentation.MainActivity

Write-Host "🎉 App is running! You can now test the login and background tracking." -ForegroundColor Cyan
