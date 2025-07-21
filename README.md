# My Android App

A modern Android application built with Kotlin, following Material Design 3 guidelines and MVVM architecture pattern.

## Features

- 🎨 **Material Design 3** - Beautiful, modern UI with dynamic color theming
- 🌙 **Dark Mode Support** - Automatic light/dark theme switching
- 🏗️ **MVVM Architecture** - Clean separation of concerns with ViewModel and LiveData
- 📱 **View Binding** - Type-safe view references
- 🔄 **Counter Functionality** - Simple increment/decrement counter with reset functionality
- 🎯 **Interactive UI** - Responsive buttons with visual feedback

## Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Framework**: Android Views with Material Design Components
- **Build System**: Gradle
- **Minimum SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/myapp/
│   │   ├── MainActivity.kt          # Main activity with UI logic
│   │   └── MainViewModel.kt         # ViewModel for state management
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml    # Main UI layout
│   │   ├── values/
│   │   │   ├── strings.xml          # String resources
│   │   │   ├── colors.xml           # Color definitions
│   │   │   └── themes.xml           # Light theme
│   │   └── values-night/
│   │       └── themes.xml           # Dark theme
│   └── AndroidManifest.xml          # App configuration
├── build.gradle                     # App-level build configuration
└── proguard-rules.pro              # ProGuard rules
```

## Building the Project

### Prerequisites

- Java 8 or higher
- Android SDK (if you want to run on device/emulator)

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Build Output

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Running the App

1. **Using Android Studio**:
   - Open the project in Android Studio
   - Wait for Gradle sync to complete
   - Click "Run" or press Ctrl+R (Cmd+R on Mac)

2. **Using Command Line**:
   ```bash
   ./gradlew installDebug
   ```

3. **Building APK Only**:
   ```bash
   ./gradlew assembleDebug
   ```

## App Functionality

The app demonstrates a simple counter with the following features:

- **Increment Button (+1)**: Increases the counter by 1
- **Decrement Button (-1)**: Decreases the counter by 1 (disabled when counter is 0)
- **Reset Button**: Resets the counter to 0
- **Floating Action Button**: Shows an informational snackbar
- **Dynamic Colors**: Counter text color changes based on value (blue for 0, green for positive)

## Architecture Details

### MVVM Pattern
- **Model**: Data and business logic (counter state)
- **View**: UI components (Activity + Layout)
- **ViewModel**: Mediates between View and Model, handles UI state

### Key Components
- `MainActivity`: Handles UI interactions and observes ViewModel
- `MainViewModel`: Manages counter state using LiveData
- Material Design Components: Cards, Buttons, Toolbar, FAB

## Dependencies

- **AndroidX Core**: Core Android libraries
- **Material Components**: Google's Material Design library
- **ConstraintLayout**: Flexible layout manager
- **Lifecycle Components**: ViewModel and LiveData
- **View Binding**: Type-safe view references

## License

This project is created for educational purposes and demonstrates modern Android development practices.

## Development Notes

- Follows Android development best practices
- Uses modern Gradle build configuration
- Implements proper resource management
- Supports both light and dark themes
- Includes proper backup and data extraction rules
- Ready for Play Store deployment with proper signing