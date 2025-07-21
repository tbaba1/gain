@echo off
setlocal EnableDelayedExpansion

REM NaijaChat APK Build Script for Windows
REM This script automates the process of building NaijaChat APK on Windows

echo.
echo 🔥 NaijaChat APK Build Script (Windows)
echo ==========================================

REM Check if we're in the right directory
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found. Make sure you're in the NaijaChat project root directory.
    pause
    exit /b 1
)

REM Check if google-services.json exists
if not exist "app\google-services.json" (
    echo [ERROR] app\google-services.json not found. Please add your Firebase configuration file.
    pause
    exit /b 1
)

echo [INFO] Starting NaijaChat APK build process...

REM Clean project
echo [INFO] Cleaning project...
call gradlew.bat clean

if !errorlevel! neq 0 (
    echo [ERROR] Clean failed. Please check your setup.
    pause
    exit /b 1
)

echo [SUCCESS] Project cleaned successfully.

REM Check build type argument
set BUILD_TYPE=debug
if "%1"=="release" (
    set BUILD_TYPE=release
    echo [WARNING] Building RELEASE APK. Make sure you have proper signing configuration.
) else if "%1"=="debug" (
    set BUILD_TYPE=debug
    echo [INFO] Building DEBUG APK.
) else if "%1"=="" (
    set BUILD_TYPE=debug
    echo [INFO] Building DEBUG APK (default).
) else (
    echo [ERROR] Invalid build type. Use 'debug' or 'release'.
    echo Usage: %0 [debug^|release]
    pause
    exit /b 1
)

REM Build APK
echo [INFO] Building %BUILD_TYPE% APK...
if "%BUILD_TYPE%"=="debug" (
    call gradlew.bat assembleDebug
) else (
    call gradlew.bat assembleRelease
)

if !errorlevel! neq 0 (
    echo [ERROR] Build failed. Please check the error messages above.
    pause
    exit /b 1
)

echo [SUCCESS] Build completed successfully!

REM Find APK file
set APK_PATH=app\build\outputs\apk\%BUILD_TYPE%
for %%f in ("%APK_PATH%\*.apk") do set APK_FILE=%%f

if exist "%APK_FILE%" (
    echo [SUCCESS] APK generated: %APK_FILE%
    
    REM Get APK size
    for %%i in ("%APK_FILE%") do set APK_SIZE=%%~zi
    set /a APK_SIZE_MB=APK_SIZE/1024/1024
    echo [INFO] APK Size: !APK_SIZE_MB! MB
    
    REM Check if ADB is available
    where adb >nul 2>nul
    if !errorlevel! equ 0 (
        echo.
        echo [INFO] ADB detected. You can install the APK using:
        echo   adb install "%APK_FILE%"
        
        REM Check if device is connected
        for /f %%i in ('adb devices ^| find /c "device"') do set DEVICE_COUNT=%%i
        if !DEVICE_COUNT! gtr 1 (
            echo.
            set /p INSTALL_NOW="[OPTION] Device detected. Install APK now? [y/N]: "
            if /i "!INSTALL_NOW!"=="y" (
                echo [INFO] Installing APK on device...
                adb install "%APK_FILE%"
                if !errorlevel! equ 0 (
                    echo [SUCCESS] APK installed successfully!
                    echo [INFO] You can now launch NaijaChat on your device.
                ) else (
                    echo [WARNING] Installation failed. You may need to enable 'Install from unknown sources' on your device.
                )
            )
        )
    ) else (
        echo [WARNING] ADB not found. Install Android SDK platform-tools to enable device installation.
    )
    
    echo.
    echo [SUCCESS] 🎉 NaijaChat APK build completed!
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    echo APK Location: %APK_FILE%
    echo APK Size: !APK_SIZE_MB! MB
    echo Build Type: %BUILD_TYPE%
    echo Package: com.naijachat.naija_chat
    echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
) else (
    echo [ERROR] APK file not found in expected location: %APK_PATH%
    pause
    exit /b 1
)

REM Check for App Bundle (release builds)
if "%BUILD_TYPE%"=="release" (
    set BUNDLE_PATH=app\build\outputs\bundle\release
    for %%f in ("!BUNDLE_PATH!\*.aab") do set BUNDLE_FILE=%%f
    
    if exist "!BUNDLE_FILE!" (
        for %%i in ("!BUNDLE_FILE!") do set BUNDLE_SIZE=%%~zi
        set /a BUNDLE_SIZE_MB=BUNDLE_SIZE/1024/1024
        echo.
        echo [SUCCESS] App Bundle also generated: !BUNDLE_FILE!
        echo [INFO] Bundle Size: !BUNDLE_SIZE_MB! MB
        echo [INFO] Use App Bundle for Google Play Store submission.
    )
)

echo.
echo [INFO] Build script completed. Happy coding! 🚀
echo.
pause