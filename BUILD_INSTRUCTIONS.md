# 📱 NaijaChat APK Build Instructions

## 🎯 Overview
This guide will help you build the NaijaChat APK file on your local machine with your Firebase configuration.

## 🛠️ Prerequisites

### **Development Environment Setup**
```bash
# Required Software:
- Android Studio (Latest version recommended)
- JDK 8 or higher
- Git
- At least 8GB RAM
- 20GB free disk space
```

### **System Requirements**
- **Windows**: Windows 10/11 (64-bit)
- **macOS**: macOS 10.14+ (64-bit)
- **Linux**: Ubuntu 18.04+ (64-bit)

## 🚀 Step-by-Step Build Process

### **Method 1: Using Android Studio (Recommended)**

1. **Install Android Studio**
   ```bash
   # Download from: https://developer.android.com/studio
   # Install with default settings
   # Launch and complete initial setup
   ```

2. **Clone/Download Project**
   ```bash
   # Option A: If using Git
   git clone <your-repository-url>
   cd naijachat
   
   # Option B: Extract downloaded ZIP
   unzip naijachat-project.zip
   cd naijachat
   ```

3. **Open in Android Studio**
   ```bash
   # File → Open → Select naijachat folder
   # Wait for Gradle sync to complete
   # Install any suggested SDK components
   ```

4. **Verify Firebase Configuration**
   ```bash
   # Ensure app/google-services.json is present
   # Check that package name matches: com.naijachat.naija_chat
   # Verify Firebase project is active
   ```

5. **Build APK**
   ```bash
   # Option A: Using Menu
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   
   # Option B: Using Terminal in Android Studio
   ./gradlew assembleDebug
   
   # Option C: For Release APK
   ./gradlew assembleRelease
   ```

6. **Locate APK File**
   ```bash
   # Debug APK:
   app/build/outputs/apk/debug/app-debug.apk
   
   # Release APK:
   app/build/outputs/apk/release/app-release.apk
   ```

### **Method 2: Command Line Build**

1. **Install Android SDK**
   ```bash
   # Download Android Command Line Tools
   # https://developer.android.com/studio/command-line
   
   # Extract and setup
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

2. **Install Required SDK Components**
   ```bash
   # Update SDK manager
   sdkmanager --update
   
   # Install required components
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   
   # Accept licenses
   sdkmanager --licenses
   ```

3. **Build Project**
   ```bash
   # Navigate to project directory
   cd naijachat
   
   # Make gradlew executable
   chmod +x gradlew
   
   # Clean project
   ./gradlew clean
   
   # Build debug APK
   ./gradlew assembleDebug
   
   # Build release APK (requires signing)
   ./gradlew assembleRelease
   ```

### **Method 3: GitHub Actions (CI/CD)**

Create `.github/workflows/build-apk.yml`:

```yaml
name: Build APK

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v2

    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Debug APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: naijachat-debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Signing Your APK (For Release)

### **Generate Signing Key**
```bash
# Create keystore
keytool -genkey -v -keystore naijachat-release-key.keystore \
        -alias naijachat -keyalg RSA -keysize 2048 -validity 10000

# Follow prompts to set passwords and information
```

### **Configure Signing in build.gradle**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('naijachat-release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'naijachat'
            keyPassword 'your_key_password'
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### **Build Signed APK**
```bash
./gradlew assembleRelease
```

## 📁 Build Output Locations

```
naijachat/
├── app/
│   └── build/
│       └── outputs/
│           ├── apk/
│           │   ├── debug/
│           │   │   └── app-debug.apk          # ← Debug APK
│           │   └── release/
│           │       └── app-release.apk       # ← Release APK
│           └── bundle/
│               └── release/
│                   └── app-release.aab       # ← App Bundle (for Play Store)
```

## 🎯 APK Types and Uses

### **Debug APK (`app-debug.apk`)**
- ✅ For testing and development
- ✅ Can be installed on any device
- ✅ Includes debug information
- ✅ Larger file size
- ❌ Not suitable for production

### **Release APK (`app-release.apk`)**
- ✅ Optimized for production
- ✅ Smaller file size
- ✅ Code obfuscation enabled
- ✅ Ready for distribution
- ⚠️ Requires proper signing

### **App Bundle (`app-release.aab`)**
- ✅ Optimized for Google Play Store
- ✅ Dynamic delivery
- ✅ Smaller download size
- ✅ Google Play recommended format

## 🔧 Build Configurations

### **Debug Build Features**
```gradle
debug {
    applicationIdSuffix ".debug"
    versionNameSuffix "-debug"
    debuggable true
    minifyEnabled false
    buildConfigField "boolean", "DEBUG_MODE", "true"
}
```

### **Release Build Features**
```gradle
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    buildConfigField "boolean", "DEBUG_MODE", "false"
    
    // Firebase Crashlytics
    firebaseCrashlytics {
        mappingFileUploadEnabled true
    }
}
```

## 🐛 Troubleshooting

### **Common Build Issues**

1. **SDK Location Not Found**
   ```bash
   # Solution: Set ANDROID_HOME environment variable
   export ANDROID_HOME=$HOME/Android/Sdk
   
   # Or create local.properties file:
   echo "sdk.dir=$HOME/Android/Sdk" > local.properties
   ```

2. **Build Tools Version**
   ```bash
   # Install specific build tools version
   sdkmanager "build-tools;34.0.0"
   ```

3. **Memory Issues**
   ```bash
   # Add to gradle.properties:
   org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

4. **Firebase Configuration**
   ```bash
   # Ensure google-services.json is in app/ directory
   # Package name must match Firebase project
   # All Firebase services must be enabled
   ```

### **Build Performance Optimization**
```gradle
# Add to gradle.properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
android.enableR8.fullMode=true
```

## 📱 Testing Your APK

### **Install APK on Device**
```bash
# Enable Developer Options and USB Debugging on device
# Connect device via USB

# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk

# Check installation
adb shell pm list packages | grep naijachat
```

### **APK Analysis**
```bash
# Analyze APK size and content
./gradlew analyzeReleaseBundle

# View APK in Android Studio
# Build → Analyze APK → Select APK file
```

## 🚀 Distribution Options

### **1. Direct APK Distribution**
- Share APK file directly
- Host on website for download
- Distribute via messaging apps

### **2. Google Play Store**
- Use App Bundle (.aab) format
- Follow Play Store guidelines
- Setup Play Console account

### **3. Alternative App Stores**
- Amazon Appstore
- Samsung Galaxy Store
- Huawei AppGallery

## 📊 Build Metrics

### **Expected Build Times**
- **Clean Build**: 3-5 minutes
- **Incremental Build**: 30-60 seconds
- **Release Build**: 5-8 minutes (with optimization)

### **APK Sizes**
- **Debug APK**: ~80-120 MB
- **Release APK**: ~40-70 MB (after optimization)
- **App Bundle**: ~30-50 MB (dynamic delivery)

## 🔒 Security Considerations

### **For Release Builds**
- ✅ Use proper code obfuscation
- ✅ Enable R8 optimization
- ✅ Remove debug logs
- ✅ Secure API keys
- ✅ Enable certificate pinning
- ✅ Use encrypted storage

### **Keystore Security**
- 🔐 Store keystore securely
- 🔐 Use strong passwords
- 🔐 Backup keystore safely
- 🔐 Never commit keystore to git

## 📞 Support

If you encounter issues building the APK:

1. **Check Android Studio logs** for detailed error messages
2. **Clean and rebuild** the project
3. **Update Android Studio** and SDK components
4. **Check Firebase configuration** matches your project
5. **Verify all dependencies** are properly resolved

---

**🎉 Once built successfully, your NaijaChat APK will be ready for installation and testing!**