#!/bin/bash

# NaijaChat APK Build Script
# This script automates the process of building NaijaChat APK

echo "🔥 NaijaChat APK Build Script"
echo "=============================="

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
if [ ! -f "gradlew" ]; then
    print_error "gradlew not found. Make sure you're in the NaijaChat project root directory."
    exit 1
fi

# Check if google-services.json exists
if [ ! -f "app/google-services.json" ]; then
    print_error "app/google-services.json not found. Please add your Firebase configuration file."
    exit 1
fi

print_status "Starting NaijaChat APK build process..."

# Make gradlew executable
print_status "Making gradlew executable..."
chmod +x gradlew

# Clean project
print_status "Cleaning project..."
./gradlew clean

if [ $? -ne 0 ]; then
    print_error "Clean failed. Please check your setup."
    exit 1
fi

print_success "Project cleaned successfully."

# Check build type argument
BUILD_TYPE="debug"
if [ "$1" = "release" ]; then
    BUILD_TYPE="release"
    print_warning "Building RELEASE APK. Make sure you have proper signing configuration."
elif [ "$1" = "debug" ] || [ -z "$1" ]; then
    BUILD_TYPE="debug"
    print_status "Building DEBUG APK (default)."
else
    print_error "Invalid build type. Use 'debug' or 'release'."
    echo "Usage: $0 [debug|release]"
    exit 1
fi

# Build APK
print_status "Building $BUILD_TYPE APK..."
if [ "$BUILD_TYPE" = "debug" ]; then
    ./gradlew assembleDebug
else
    ./gradlew assembleRelease
fi

if [ $? -ne 0 ]; then
    print_error "Build failed. Please check the error messages above."
    exit 1
fi

print_success "Build completed successfully!"

# Find and display APK location
APK_PATH="app/build/outputs/apk/$BUILD_TYPE"
APK_FILE=$(find $APK_PATH -name "*.apk" | head -1)

if [ -f "$APK_FILE" ]; then
    print_success "APK generated: $APK_FILE"
    
    # Get APK size
    APK_SIZE=$(du -h "$APK_FILE" | cut -f1)
    print_status "APK Size: $APK_SIZE"
    
    # Check if ADB is available for installation
    if command -v adb &> /dev/null; then
        echo ""
        print_status "ADB detected. You can install the APK using:"
        echo "  adb install \"$APK_FILE\""
        
        # Check if device is connected
        DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
        if [ $DEVICES -gt 0 ]; then
            echo ""
            read -p "$(echo -e ${YELLOW}[OPTION]${NC} Device detected. Install APK now? [y/N]: )" -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                print_status "Installing APK on device..."
                adb install "$APK_FILE"
                if [ $? -eq 0 ]; then
                    print_success "APK installed successfully!"
                    print_status "You can now launch NaijaChat on your device."
                else
                    print_warning "Installation failed. You may need to enable 'Install from unknown sources' on your device."
                fi
            fi
        fi
    else
        print_warning "ADB not found. Install Android SDK platform-tools to enable device installation."
    fi
    
    echo ""
    print_success "🎉 NaijaChat APK build completed!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "APK Location: $APK_FILE"
    echo "APK Size: $APK_SIZE"
    echo "Build Type: $BUILD_TYPE"
    echo "Package: com.naijachat.naija_chat"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
else
    print_error "APK file not found in expected location: $APK_PATH"
    exit 1
fi

# Check for other build outputs
BUNDLE_PATH="app/build/outputs/bundle/release"
if [ -d "$BUNDLE_PATH" ] && [ "$BUILD_TYPE" = "release" ]; then
    BUNDLE_FILE=$(find $BUNDLE_PATH -name "*.aab" | head -1)
    if [ -f "$BUNDLE_FILE" ]; then
        BUNDLE_SIZE=$(du -h "$BUNDLE_FILE" | cut -f1)
        echo ""
        print_success "App Bundle also generated: $BUNDLE_FILE"
        print_status "Bundle Size: $BUNDLE_SIZE"
        print_status "Use App Bundle for Google Play Store submission."
    fi
fi

echo ""
print_status "Build script completed. Happy coding! 🚀"