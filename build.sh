#!/bin/bash

# VibeStream Build Script
# This script automates the build process for the VibeStream Android app

set -e  # Exit on any error

echo "🎵 VibeStream Build Script"
echo "========================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    print_error "Please run this script from the VibeStream project root directory"
    exit 1
fi

print_status "Starting VibeStream build process..."

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    print_warning "ANDROID_HOME not set. Please set it to your Android SDK path."
    print_warning "Example: export ANDROID_HOME=/path/to/android/sdk"
fi

# Check for Java
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "17" ]; then
    print_warning "Java 17 or higher is recommended. Current version: $JAVA_VERSION"
fi

# Function to build with gradlew
build_with_gradlew() {
    if [ -f "./gradlew" ]; then
        print_status "Using Gradle wrapper..."
        chmod +x ./gradlew
        
        if [ "$1" = "clean" ]; then
            print_status "Cleaning project..."
            ./gradlew clean
        fi
        
        print_status "Building debug APK..."
        ./gradlew assembleDebug --no-daemon --warning-mode all
        
        if [ $? -eq 0 ]; then
            print_success "Build completed successfully!"
            
            # Find the generated APK
            APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
            if [ -n "$APK_PATH" ]; then
                APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
                print_success "APK generated: $APK_PATH ($APK_SIZE)"
                
                # Show APK info
                print_status "APK Information:"
                echo "  📍 Location: $APK_PATH"
                echo "  📦 Size: $APK_SIZE"
                echo "  🎯 Target: Debug build"
                echo "  📱 Min SDK: 26 (Android 8.0)"
                echo "  🚀 Target SDK: 34 (Android 14)"
            fi
        else
            print_error "Build failed!"
            exit 1
        fi
    else
        print_error "Gradle wrapper not found. Please ensure gradlew exists."
        exit 1
    fi
}

# Function to build with system gradle
build_with_gradle() {
    if command -v gradle &> /dev/null; then
        print_status "Using system Gradle..."
        
        if [ "$1" = "clean" ]; then
            print_status "Cleaning project..."
            gradle clean
        fi
        
        print_status "Building debug APK..."
        gradle assembleDebug --no-daemon --warning-mode all
        
        if [ $? -eq 0 ]; then
            print_success "Build completed successfully!"
        else
            print_error "Build failed!"
            exit 1
        fi
    else
        print_error "Gradle not found in PATH"
        return 1
    fi
}

# Parse command line arguments
CLEAN_BUILD=false
RELEASE_BUILD=false
RUN_TESTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --release)
            RELEASE_BUILD=true
            shift
            ;;
        --test)
            RUN_TESTS=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --clean     Clean build (removes previous build artifacts)"
            echo "  --release   Build release APK (requires signing configuration)"
            echo "  --test      Run unit tests before building"
            echo "  --help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Build debug APK"
            echo "  $0 --clean           # Clean and build debug APK"
            echo "  $0 --test --clean    # Run tests, clean, and build"
            echo "  $0 --release         # Build release APK"
            exit 0
            ;;
        *)
            print_warning "Unknown option: $1"
            shift
            ;;
    esac
done

# Show build configuration
print_status "Build Configuration:"
echo "  🧹 Clean build: $CLEAN_BUILD"
echo "  🚀 Release build: $RELEASE_BUILD"
echo "  🧪 Run tests: $RUN_TESTS"
echo ""

# Run tests if requested
if [ "$RUN_TESTS" = true ]; then
    print_status "Running unit tests..."
    if [ -f "./gradlew" ]; then
        ./gradlew testDebugUnitTest --no-daemon
    else
        gradle testDebugUnitTest --no-daemon
    fi
    
    if [ $? -eq 0 ]; then
        print_success "All tests passed!"
    else
        print_error "Tests failed!"
        exit 1
    fi
fi

# Build the app
if [ "$CLEAN_BUILD" = true ]; then
    build_with_gradlew clean || build_with_gradle clean
else
    build_with_gradlew || build_with_gradle
fi

# Additional build steps for release
if [ "$RELEASE_BUILD" = true ]; then
    print_warning "Release build requested but signing configuration may be required."
    print_status "Please ensure you have configured signing in app/build.gradle.kts"
fi

print_success "VibeStream build process completed!"
echo ""
print_status "Next steps:"
echo "  📱 Install APK: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "  🧪 Run tests: ./gradlew test"
echo "  📊 Generate report: ./gradlew assembleDebug --scan"
echo ""
echo "🎵 Enjoy your VibeStream media player! 🎵"#!/bin/bash

# VibeStream Build Script
# This script automates the build process for the VibeStream Android app

set -e  # Exit on any error

echo "🎵 VibeStream Build Script"
echo "========================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    print_error "Please run this script from the VibeStream project root directory"
    exit 1
fi

print_status "Starting VibeStream build process..."

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    print_warning "ANDROID_HOME not set. Please set it to your Android SDK path."
    print_warning "Example: export ANDROID_HOME=/path/to/android/sdk"
fi

# Check for Java
if ! command -v java &> /dev/null; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "17" ]; then
    print_warning "Java 17 or higher is recommended. Current version: $JAVA_VERSION"
fi

# Function to build with gradlew
build_with_gradlew() {
    if [ -f "./gradlew" ]; then
        print_status "Using Gradle wrapper..."
        chmod +x ./gradlew
        
        if [ "$1" = "clean" ]; then
            print_status "Cleaning project..."
            ./gradlew clean
        fi
        
        print_status "Building debug APK..."
        ./gradlew assembleDebug --no-daemon --warning-mode all
        
        if [ $? -eq 0 ]; then
            print_success "Build completed successfully!"
            
            # Find the generated APK
            APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
            if [ -n "$APK_PATH" ]; then
                APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
                print_success "APK generated: $APK_PATH ($APK_SIZE)"
                
                # Show APK info
                print_status "APK Information:"
                echo "  📍 Location: $APK_PATH"
                echo "  📦 Size: $APK_SIZE"
                echo "  🎯 Target: Debug build"
                echo "  📱 Min SDK: 26 (Android 8.0)"
                echo "  🚀 Target SDK: 34 (Android 14)"
            fi
        else
            print_error "Build failed!"
            exit 1
        fi
    else
        print_error "Gradle wrapper not found. Please ensure gradlew exists."
        exit 1
    fi
}

# Function to build with system gradle
build_with_gradle() {
    if command -v gradle &> /dev/null; then
        print_status "Using system Gradle..."
        
        if [ "$1" = "clean" ]; then
            print_status "Cleaning project..."
            gradle clean
        fi
        
        print_status "Building debug APK..."
        gradle assembleDebug --no-daemon --warning-mode all
        
        if [ $? -eq 0 ]; then
            print_success "Build completed successfully!"
        else
            print_error "Build failed!"
            exit 1
        fi
    else
        print_error "Gradle not found in PATH"
        return 1
    fi
}

# Parse command line arguments
CLEAN_BUILD=false
RELEASE_BUILD=false
RUN_TESTS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --release)
            RELEASE_BUILD=true
            shift
            ;;
        --test)
            RUN_TESTS=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --clean     Clean build (removes previous build artifacts)"
            echo "  --release   Build release APK (requires signing configuration)"
            echo "  --test      Run unit tests before building"
            echo "  --help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                    # Build debug APK"
            echo "  $0 --clean           # Clean and build debug APK"
            echo "  $0 --test --clean    # Run tests, clean, and build"
            echo "  $0 --release         # Build release APK"
            exit 0
            ;;
        *)
            print_warning "Unknown option: $1"
            shift
            ;;
    esac
done

# Show build configuration
print_status "Build Configuration:"
echo "  🧹 Clean build: $CLEAN_BUILD"
echo "  🚀 Release build: $RELEASE_BUILD"
echo "  🧪 Run tests: $RUN_TESTS"
echo ""

# Run tests if requested
if [ "$RUN_TESTS" = true ]; then
    print_status "Running unit tests..."
    if [ -f "./gradlew" ]; then
        ./gradlew testDebugUnitTest --no-daemon
    else
        gradle testDebugUnitTest --no-daemon
    fi
    
    if [ $? -eq 0 ]; then
        print_success "All tests passed!"
    else
        print_error "Tests failed!"
        exit 1
    fi
fi

# Build the app
if [ "$CLEAN_BUILD" = true ]; then
    build_with_gradlew clean || build_with_gradle clean
else
    build_with_gradlew || build_with_gradle
fi

# Additional build steps for release
if [ "$RELEASE_BUILD" = true ]; then
    print_warning "Release build requested but signing configuration may be required."
    print_status "Please ensure you have configured signing in app/build.gradle.kts"
fi

print_success "VibeStream build process completed!"
echo ""
print_status "Next steps:"
echo "  📱 Install APK: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "  🧪 Run tests: ./gradlew test"
echo "  📊 Generate report: ./gradlew assembleDebug --scan"
echo ""
echo "🎵 Enjoy your VibeStream media player! 🎵"