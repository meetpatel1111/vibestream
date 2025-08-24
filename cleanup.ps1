# =================================================
# Universal Cleanup Script (Android + Node.js)
# =================================================

function Get-FolderSize($path) {
    if (Test-Path $path) {
        (Get-ChildItem -Recurse -Force $path -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    } else {
        return 0
    }
}

$totalCleaned = 0

function Clean-Folder($path) {
    if (Test-Path $path) {
        $size = Get-FolderSize $path
        if ($size -gt 0) {
            Write-Output "🧹 Cleaning $path ($( [math]::Round($size/1MB,2) ) MB)..."
            Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
            $GLOBALS:totalCleaned += $size
            Write-Output "✅ Done"
        }
    }
}

Write-Output "🚀 Starting Universal Cleanup..."

# Android: Gradle caches
Clean-Folder "$env:USERPROFILE\.gradle\caches"

# Node.js: npm, yarn, pnpm caches
Clean-Folder "$env:USERPROFILE\.npm"
Clean-Folder "$env:APPDATA\npm-cache"
Clean-Folder "$env:LOCALAPPDATA\Yarn\Cache"
Clean-Folder "$env:USERPROFILE\.pnpm-store"

# Project build folders (Android & Node.js)
foreach ($folder in "build","dist","node_modules",".next",".nuxt",".svelte-kit",".angular",".cache",".turbo") {
    Get-ChildItem -Recurse -Directory -Filter $folder -ErrorAction SilentlyContinue | ForEach-Object { Clean-Folder $_.FullName }
}

# Logs
foreach ($file in "npm-debug.log","yarn-error.log") {
    if (Test-Path $file) {
        $size = (Get-Item $file).Length
        Write-Output "🧹 Cleaning $file ($( [math]::Round($size/1MB,2) ) MB)..."
        Remove-Item -Force $file
        $totalCleaned += $size
    }
}

Write-Output "---------------------------------"
Write-Output ("💾 Total Space Reclaimed: " + [math]::Round($totalCleaned/1MB,2) + " MB")
Write-Output "✅ Universal Cleanup Complete!"
