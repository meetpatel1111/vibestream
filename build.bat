@echo off
setlocal enabledelayedexpansion

REM VibeStream Build Script for Windows
REM This script automates the build process for the VibeStream Android app

echo.
echo 🎵 VibeStream Build Script
echo =========================
echo.

REM Check if we're in the right directory
if not exist "settings.gradle.kts" (
    echo [ERROR] Please run this script from the VibeStream project root directory
    exit /b 1
)

echo [INFO] Starting VibeStream build process...

REM Check for Android SDK
if "%ANDROID_HOME%"=="" (
    echo [WARNING] ANDROID_HOME not set. Please set it to your Android SDK path.
    echo [WARNING] Example: set ANDROID_HOME=C:\Android\Sdk
)

REM Check for Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    exit /b 1
)

REM Parse command line arguments
set CLEAN_BUILD=false
set RELEASE_BUILD=false
set RUN_TESTS=false

:parse_args
if "%~1"=="" goto :done_parsing
if "%~1"=="--clean" (
    set CLEAN_BUILD=true
    shift
    goto :parse_args
)
if "%~1"=="--release" (
    set RELEASE_BUILD=true
    shift
    goto :parse_args
)
if "%~1"=="--test" (
    set RUN_TESTS=true
    shift
    goto :parse_args
)
if "%~1"=="--help" (
    echo Usage: %0 [OPTIONS]
    echo.
    echo Options:
    echo   --clean     Clean build ^(removes previous build artifacts^)
    echo   --release   Build release APK ^(requires signing configuration^)
    echo   --test      Run unit tests before building
    echo   --help      Show this help message
    echo.
    echo Examples:
    echo   %0                    # Build debug APK
    echo   %0 --clean           # Clean and build debug APK
    echo   %0 --test --clean    # Run tests, clean, and build
    echo   %0 --release         # Build release APK
    exit /b 0
)
echo [WARNING] Unknown option: %~1
shift
goto :parse_args

:done_parsing

REM Show build configuration
echo [INFO] Build Configuration:
echo   🧹 Clean build: !CLEAN_BUILD!
echo   🚀 Release build: !RELEASE_BUILD!
echo   🧪 Run tests: !RUN_TESTS!
echo.

REM Run tests if requested
if "!RUN_TESTS!"=="true" (
    echo [INFO] Running unit tests...
    if exist "gradlew.bat" (
        call gradlew.bat testDebugUnitTest --no-daemon
    ) else (
        gradle testDebugUnitTest --no-daemon
    )
    
    if !errorlevel! neq 0 (
        echo [ERROR] Tests failed!
        exit /b 1
    )
    echo [SUCCESS] All tests passed!
)

REM Clean if requested
if "!CLEAN_BUILD!"=="true" (
    echo [INFO] Cleaning project...
    if exist "gradlew.bat" (
        call gradlew.bat clean
    ) else (
        gradle clean
    )
)

REM Build the app
echo [INFO] Building debug APK...

if exist "gradlew.bat" (
    echo [INFO] Using Gradle wrapper...
    call gradlew.bat assembleDebug --no-daemon --warning-mode all
) else (
    echo [INFO] Using system Gradle...
    gradle assembleDebug --no-daemon --warning-mode all
)

if !errorlevel! neq 0 (
    echo [ERROR] Build failed!
    exit /b 1
)

echo [SUCCESS] Build completed successfully!

REM Find the generated APK
for /r "app\build\outputs\apk\debug" %%f in (*.apk) do (
    if exist "%%f" (
        set APK_PATH=%%f
        goto :found_apk
    )
)

:found_apk
if defined APK_PATH (
    echo [SUCCESS] APK generated: !APK_PATH!
    
    REM Get file size
    for %%A in ("!APK_PATH!") do set APK_SIZE=%%~zA
    set /a APK_SIZE_MB=!APK_SIZE!/1024/1024
    
    echo [INFO] APK Information:
    echo   📍 Location: !APK_PATH!
    echo   📦 Size: !APK_SIZE_MB! MB
    echo   🎯 Target: Debug build
    echo   📱 Min SDK: 26 ^(Android 8.0^)
    echo   🚀 Target SDK: 34 ^(Android 14^)
)

REM Additional build steps for release
if "!RELEASE_BUILD!"=="true" (
    echo [WARNING] Release build requested but signing configuration may be required.
    echo [INFO] Please ensure you have configured signing in app\build.gradle.kts
)

echo.
echo [SUCCESS] VibeStream build process completed!
echo.
echo [INFO] Next steps:
echo   📱 Install APK: adb install app\build\outputs\apk\debug\app-debug.apk
echo   🧪 Run tests: gradlew.bat test
echo   📊 Generate report: gradlew.bat assembleDebug --scan
echo.
echo 🎵 Enjoy your VibeStream media player! 🎵

endlocal