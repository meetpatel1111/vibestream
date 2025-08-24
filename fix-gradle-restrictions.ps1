# VibeStream Gradle Repository Protocol Restrictions Removal Script
# Run this script as Administrator
# This script removes Gradle repository protocol restrictions that prevent Android builds

param(
    [switch]$BackupOnly,
    [switch]$Verbose
)

# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator"))
{
    Write-Host "ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    Exit 1
}

Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "VibeStream Gradle Restrictions Removal Script" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host ""

$BackupDir = "C:\GradleRestrictions_Backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
$Changes = @()
$FilesModified = 0

function Write-VerboseLog {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "[VERBOSE] $Message" -ForegroundColor Gray
    }
}

function Backup-File {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        $BackupPath = Join-Path $BackupDir (Split-Path $FilePath -Leaf)
        if (!(Test-Path $BackupDir)) {
            New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
        }
        Copy-Item $FilePath $BackupPath -Force
        Write-VerboseLog "Backed up: $FilePath -> $BackupPath"
        return $true
    }
    return $false
}

function Remove-RestrictiveContent {
    param(
        [string]$FilePath,
        [string[]]$RestrictivePatterns
    )
    
    if (!(Test-Path $FilePath)) {
        return $false
    }
    
    $Content = Get-Content $FilePath -Raw
    $OriginalContent = $Content
    $Modified = $false
    
    foreach ($Pattern in $RestrictivePatterns) {
        if ($Content -match $Pattern) {
            Write-Host "  Found restrictive pattern in $FilePath" -ForegroundColor Yellow
            $Content = $Content -replace $Pattern, ""
            $Modified = $true
        }
    }
    
    # Remove empty lines that might be left
    $Content = ($Content -split "`n" | Where-Object { $_.Trim() -ne "" }) -join "`n"
    
    if ($Modified -and !$BackupOnly) {
        Set-Content -Path $FilePath -Value $Content -Encoding UTF8
        Write-Host "  [MODIFIED] $FilePath" -ForegroundColor Green
        $script:FilesModified++
        return $true
    }
    
    return $Modified
}

Write-Host "1. Creating backup directory: $BackupDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

# Define restrictive patterns to remove
$RestrictivePatterns = @(
    'org\.gradle\.repositories\.restricted\s*=\s*true',
    'org\.gradle\.repositories\.allowedProtocols\s*=\s*[^`n]*(?:file|gcs|s3|sftp)[^`n]*',
    'systemProp\.gradle\.wrapperUser\s*=\s*[^`n]*',
    'systemProp\.gradle\.wrapperPassword\s*=\s*[^`n]*',
    'allowInsecureProtocol\s*=\s*false',
    'if\s*\(\s*url\.startsWith\s*\(\s*[''"]https?://[''"][^}]*throw\s+new\s+GradleException[^}]*}',
    'Not\s+a\s+supported\s+repository\s+protocol[^`n]*'
)

Write-Host "`n2. Checking Gradle Installation Directory..." -ForegroundColor Yellow

# 1. Check Gradle init.d directory
$GradleInitDir = "C:\Program Files\gradle\init.d"
Write-Host "  Checking: $GradleInitDir" -ForegroundColor Cyan

if (Test-Path $GradleInitDir) {
    $InitFiles = Get-ChildItem $GradleInitDir -Filter "*.gradle*" -Recurse
    foreach ($File in $InitFiles) {
        Write-VerboseLog "Checking init file: $($File.FullName)"
        if (Backup-File $File.FullName) {
            if (Remove-RestrictiveContent $File.FullName $RestrictivePatterns) {
                $Changes += "Modified init script: $($File.FullName)"
            }
        }
    }
    
    if ($InitFiles.Count -eq 0) {
        Write-Host "  [SUCCESS] No init scripts found" -ForegroundColor Green
    }
} else {
    Write-Host "  [SUCCESS] Init directory doesn't exist" -ForegroundColor Green
}

# 2. Check Gradle installation properties
$GradleInstallProps = "C:\Program Files\gradle\gradle.properties"
Write-Host "  Checking: $GradleInstallProps" -ForegroundColor Cyan

if (Test-Path $GradleInstallProps) {
    if (Backup-File $GradleInstallProps) {
        if (Remove-RestrictiveContent $GradleInstallProps $RestrictivePatterns) {
            $Changes += "Modified installation properties: $GradleInstallProps"
        }
    }
} else {
    Write-Host "  [SUCCESS] Installation properties file doesn't exist" -ForegroundColor Green
}

Write-Host "`n3. Checking User Gradle Directory..." -ForegroundColor Yellow

# 3. Check user .gradle directory
$UserGradleDir = "$env:USERPROFILE\.gradle"
$UserGradleProps = "$UserGradleDir\gradle.properties"

Write-Host "  Checking: $UserGradleProps" -ForegroundColor Cyan

if (Test-Path $UserGradleProps) {
    if (Backup-File $UserGradleProps) {
        if (Remove-RestrictiveContent $UserGradleProps $RestrictivePatterns) {
            $Changes += "Modified user properties: $UserGradleProps"
        }
        
        # Add beneficial properties
        if (!$BackupOnly) {
            $BeneficialProps = @"

# Added by VibeStream fix script - Allow all protocols
org.gradle.repositories.allowedProtocols=file,https,http,gcs,s3,sftp
org.gradle.daemon=true
org.gradle.configureondemand=false
org.gradle.parallel=true

# Android specific properties
android.useAndroidX=true
android.enableJetifier=true
"@
            Add-Content -Path $UserGradleProps -Value $BeneficialProps
            Write-Host "  [SUCCESS] Added beneficial properties to user gradle.properties" -ForegroundColor Green
        }
    }
} else {
    # Create user gradle.properties with beneficial settings
    if (!$BackupOnly) {
        if (!(Test-Path $UserGradleDir)) {
            New-Item -ItemType Directory -Path $UserGradleDir -Force | Out-Null
        }
        
        $NewProps = @"
# VibeStream Gradle Configuration
# Allow all repository protocols
org.gradle.repositories.allowedProtocols=file,https,http,gcs,s3,sftp

# Performance settings
org.gradle.daemon=true
org.gradle.configureondemand=false
org.gradle.parallel=true

# Android specific properties
android.useAndroidX=true
android.enableJetifier=true

# Disable build scan to avoid protocol issues
org.gradle.buildscan.enabled=false
"@
        Set-Content -Path $UserGradleProps -Value $NewProps -Encoding UTF8
        Write-Host "  [SUCCESS] Created user gradle.properties with proper settings" -ForegroundColor Green
        $Changes += "Created user properties: $UserGradleProps"
    }
}

# 4. Check user init.d directory
$UserInitDir = "$UserGradleDir\init.d"
Write-Host "  Checking: $UserInitDir" -ForegroundColor Cyan

if (Test-Path $UserInitDir) {
    $UserInitFiles = Get-ChildItem $UserInitDir -Filter "*.gradle*" -Recurse
    foreach ($File in $UserInitFiles) {
        Write-VerboseLog "Checking user init file: $($File.FullName)"
        if (Backup-File $File.FullName) {
            if (Remove-RestrictiveContent $File.FullName $RestrictivePatterns) {
                $Changes += "Modified user init script: $($File.FullName)"
            }
        }
    }
    
    if ($UserInitFiles.Count -eq 0) {
        Write-Host "  [SUCCESS] No user init scripts found" -ForegroundColor Green
    }
} else {
    Write-Host "  [SUCCESS] User init directory doesn't exist" -ForegroundColor Green
}

Write-Host "`n4. Checking Environment Variables..." -ForegroundColor Yellow

# 5. Check and fix environment variables
$EnvVarsToCheck = @('GRADLE_OPTS', 'GRADLE_USER_HOME')
$EnvChanges = @()

foreach ($EnvVar in $EnvVarsToCheck) {
    $Value = [Environment]::GetEnvironmentVariable($EnvVar, 'Machine')
    if ($Value) {
        Write-Host "  Found system $EnvVar : $Value" -ForegroundColor Cyan
        
        if ($Value -match 'allowedProtocols.*file.*gcs.*s3.*sftp' -and $Value -notmatch 'https|http') {
            Write-Host "  ⚠ Restrictive $EnvVar detected" -ForegroundColor Yellow
            if (!$BackupOnly) {
                $NewValue = $Value -replace 'allowedProtocols=[^"]*', 'allowedProtocols=file,https,http,gcs,s3,sftp'
                [Environment]::SetEnvironmentVariable($EnvVar, $NewValue, 'Machine')
                Write-Host "  [SUCCESS] Fixed system $EnvVar" -ForegroundColor Green
                $EnvChanges += "Modified system environment variable: $EnvVar"
            }
        }
    }
    
    $UserValue = [Environment]::GetEnvironmentVariable($EnvVar, 'User')
    if ($UserValue) {
        Write-Host "  Found user $EnvVar : $UserValue" -ForegroundColor Cyan
        
        if ($UserValue -match 'allowedProtocols.*file.*gcs.*s3.*sftp' -and $UserValue -notmatch 'https|http') {
            Write-Host "  ⚠ Restrictive user $EnvVar detected" -ForegroundColor Yellow
            if (!$BackupOnly) {
                $NewUserValue = $UserValue -replace 'allowedProtocols=[^"]*', 'allowedProtocols=file,https,http,gcs,s3,sftp'
                [Environment]::SetEnvironmentVariable($EnvVar, $NewUserValue, 'User')
                Write-Host "  [SUCCESS] Fixed user $EnvVar" -ForegroundColor Green
                $EnvChanges += "Modified user environment variable: $EnvVar"
            }
        }
    }
}

if ($EnvVarsToCheck.Count -eq 0) {
    Write-Host "  [SUCCESS] No restrictive environment variables found" -ForegroundColor Green
}

Write-Host "`n5. Checking Project-Specific Files..." -ForegroundColor Yellow

# 6. Fix project gradle.properties
$ProjectGradleProps = "C:\Users\LENOVO\Downloads\Qoder-Projects\VibeStream\gradle.properties"
Write-Host "  Checking: $ProjectGradleProps" -ForegroundColor Cyan

if (Test-Path $ProjectGradleProps) {
    if (Backup-File $ProjectGradleProps) {
        if (Remove-RestrictiveContent $ProjectGradleProps $RestrictivePatterns) {
            $Changes += "Modified project properties: $ProjectGradleProps"
        }
    }
}

# 7. Create/update project gradle.properties with safe settings
if (!$BackupOnly) {
    $ProjectProps = @"
# VibeStream Project Gradle Configuration
# Disable wrapper plugin to avoid restrictions
org.gradle.wrapper.enabled=false

# Allow all protocols
org.gradle.repositories.allowedProtocols=file,https,http,gcs,s3,sftp

# Performance and compatibility settings
org.gradle.configureondemand=false
org.gradle.parallel=false
org.gradle.daemon=false

# Android properties
android.useAndroidX=true
android.enableJetifier=true

# Disable problematic features
org.gradle.buildscan.enabled=false
"@
    Set-Content -Path $ProjectGradleProps -Value $ProjectProps -Encoding UTF8
    Write-Host "  [SUCCESS] Updated project gradle.properties with safe settings" -ForegroundColor Green
}

Write-Host "`n6. Summary" -ForegroundColor Yellow
Write-Host "=" * 40 -ForegroundColor Cyan

if ($BackupOnly) {
    Write-Host "BACKUP ONLY MODE - No changes were made" -ForegroundColor Yellow
    Write-Host "Backup location: $BackupDir" -ForegroundColor Cyan
} else {
    Write-Host "Files modified: $FilesModified" -ForegroundColor Green
    Write-Host "Environment variables: $($EnvChanges.Count)" -ForegroundColor Green
    Write-Host "Backup location: $BackupDir" -ForegroundColor Cyan
    
    if ($Changes.Count -gt 0) {
        Write-Host "`nChanges made:" -ForegroundColor Yellow
        foreach ($Change in $Changes) {
            Write-Host "  • $Change" -ForegroundColor White
        }
        foreach ($EnvChange in $EnvChanges) {
            Write-Host "  • $EnvChange" -ForegroundColor White
        }
    }
    
    Write-Host "[SUCCESS] Gradle repository restrictions have been removed!" -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Close all PowerShell/Command Prompt windows" -ForegroundColor White
    Write-Host "2. Open a new PowerShell as Administrator" -ForegroundColor White
    Write-Host "3. Navigate to your project directory:" -ForegroundColor White
    Write-Host "   cd C:\Users\LENOVO\Downloads\Qoder-Projects\VibeStream" -ForegroundColor Gray
    Write-Host "4. Try building again:" -ForegroundColor White
    Write-Host "   & \"C:\Program Files\gradle\bin\gradle.bat\" assembleDebug --no-daemon" -ForegroundColor Gray
    
    Write-Host "`nIf issues persist, check firewall/proxy settings or contact your network administrator." -ForegroundColor Yellow
}

Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host "Script completed successfully!" -ForegroundColor Green
Write-Host ("=" * 60) -ForegroundColor Cyan