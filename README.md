# 🎬 9jaChat - Nigeria's Premier TikTok Clone

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Material Design](https://img.shields.io/badge/Design-Material%203-purple.svg)](https://m3.material.io)
[![Nigerian](https://img.shields.io/badge/Made%20in-Nigeria-008751.svg)](https://nigeria.gov.ng)

**9jaChat** is a fully-featured TikTok-style social media application designed specifically for Nigerian users, featuring short-form video content, social interactions, and local cultural elements.

## 🌟 Features

### 📱 **Core TikTok Features**
- **Vertical Video Feed** - Swipeable full-screen video experience
- **Video Recording** - Professional-quality video capture with effects
- **Social Interactions** - Like, comment, share, follow system
- **Discover Content** - Trending videos, hashtags, and users
- **User Profiles** - Complete profile management with stats
- **Direct Messaging** - Private chat and notifications

### 🇳🇬 **Nigerian-Focused Features**
- **Naija Trending** - Local trending content and hashtags
- **Afrobeats Integration** - Music library featuring Nigerian artists
- **Nollywood Content** - Movie clips and entertainment content
- **Local Languages** - Support for Pidgin English and local dialects
- **Nigerian Holidays** - Special themes and content for local celebrations

### 🎥 **Advanced Video Features**
- **HD Recording** - Up to 1080p video quality
- **Video Effects** - Filters, stickers, and AR effects
- **Speed Control** - Slow motion and fast forward recording
- **Timer Recording** - Hands-free video capture
- **Duet & Stitch** - Collaborative video creation
- **Live Streaming** - Real-time video broadcasting

### 📲 **Social Features**
- **Following System** - Follow your favorite creators
- **Comments & Replies** - Engage with threaded conversations
- **Video Sharing** - Share to WhatsApp, Instagram, Twitter
- **Private Messaging** - Direct chat with other users
- **Push Notifications** - Real-time updates and alerts
- **Content Moderation** - Report and block inappropriate content

### 🎨 **Modern UI/UX**
- **Material Design 3** - Latest Google design guidelines
- **Dark Theme** - Eye-friendly dark mode interface
- **Smooth Animations** - Fluid transitions and micro-interactions
- **Responsive Design** - Works on all Android screen sizes
- **Nigerian Branding** - Green and white color scheme with local touches

## 🏗️ Technical Architecture

### **Tech Stack**
- **Language**: Kotlin 100%
- **Architecture**: MVVM with Repository Pattern
- **UI**: Android Views + Material Design Components
- **Video**: ExoPlayer + CameraX
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Glide with transformations
- **Database**: Room (local) + Firebase (cloud)
- **Authentication**: Firebase Auth
- **Storage**: Firebase Storage + Cloud Storage

### **Key Libraries**
```kotlin
// Core Android
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.5'

// Video & Camera
implementation 'androidx.camera:camera-core:1.3.1'
implementation 'com.google.android.exoplayer:exoplayer:2.19.1'

// UI & Design
implementation 'com.google.android.material:material:1.10.0'
implementation 'com.airbnb.android:lottie:6.2.0'
implementation 'com.facebook.shimmer:shimmer:0.5.0'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

### **Project Structure**
```
app/src/main/
├── java/com/nineja/chat/
│   ├── ui/
│   │   ├── home/              # Main video feed
│   │   ├── discover/          # Search and trending
│   │   ├── camera/            # Video recording
│   │   ├── inbox/             # Messages and notifications
│   │   └── profile/           # User profiles
│   ├── model/                 # Data models
│   ├── adapter/               # RecyclerView adapters
│   ├── utils/                 # Utility classes
│   └── network/               # API and networking
├── res/
│   ├── layout/                # XML layouts
│   ├── values/                # Colors, strings, themes
│   ├── drawable/              # Icons and graphics
│   └── navigation/            # Navigation graphs
└── AndroidManifest.xml        # App configuration
```

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Java 8 or Kotlin 1.9+
- Git for version control

### **Installation**

1. **Clone the Repository**
```bash
git clone https://github.com/yourusername/9jachat.git
cd 9jachat
```

2. **Open in Android Studio**
- Launch Android Studio
- Select "Open an existing Android Studio project"
- Navigate to the cloned directory and select it

3. **Sync Dependencies**
- Android Studio will automatically sync Gradle dependencies
- Wait for the sync to complete

4. **Build and Run**
```bash
# Using Gradle wrapper
./gradlew assembleDebug

# Or use Android Studio's Run button
```

### **Required Permissions**
The app requires the following permissions:
- 📹 **Camera** - For video recording
- 🎤 **Microphone** - For audio recording
- 📁 **Storage** - For saving videos and photos
- 🌐 **Internet** - For content upload/download
- 📳 **Notifications** - For push notifications

## 📁 Key Components

### **MainActivity.kt**
Main activity with bottom navigation and fragment management
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    // Permission handling, navigation setup, etc.
}
```

### **HomeFragment.kt**
TikTok-style vertical video feed with auto-play
```kotlin
class HomeFragment : Fragment() {
    private lateinit var videoAdapter: VideoFeedAdapter
    
    // Video feed, infinite scroll, auto-play logic
}
```

### **CameraActivity.kt**
Professional video recording with effects and filters
```kotlin
class CameraActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    
    // Camera setup, recording, effects, upload
}
```

### **Video.kt & User.kt**
Comprehensive data models for content and users
```kotlin
@Parcelize
data class Video(
    val id: String,
    val videoUrl: String,
    val user: User,
    val likesCount: Int,
    // ... comprehensive properties
) : Parcelable
```

## 🎨 UI Components

### **Color Scheme**
```xml
<!-- Nigerian-inspired colors -->
<color name="primary_green">#00F2EA</color>    <!-- Teal accent -->
<color name="primary_pink">#FF0050</color>     <!-- Brand pink -->
<color name="nigeria_green">#008751</color>    <!-- Flag green -->
<color name="afrobeats_orange">#FF6B35</color> <!-- Cultural orange -->
```

### **Typography**
- **Headlines**: Sans-serif Black for impact
- **Body Text**: Sans-serif Regular for readability
- **Captions**: Sans-serif Light for secondary info

### **Navigation**
```xml
<!-- Bottom Navigation with 5 tabs -->
<item android:id="@+id/nav_home" android:title="Home" />
<item android:id="@+id/nav_discover" android:title="Discover" />
<item android:id="@+id/nav_camera" android:title="Camera" />
<item android:id="@+id/nav_inbox" android:title="Inbox" />
<item android:id="@+id/nav_profile" android:title="Profile" />
```

## 🔧 Development

### **Code Style**
- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Prefer `val` over `var` when possible

### **Git Workflow**
```bash
# Feature development
git checkout -b feature/new-feature
git commit -m "feat: add new feature"
git push origin feature/new-feature

# Bug fixes
git checkout -b fix/bug-description
git commit -m "fix: resolve bug description"
```

### **Testing**
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 📱 Screenshots

| Home Feed | Camera | Profile | Discover |
|-----------|--------|---------|----------|
| ![Home](screenshots/home.png) | ![Camera](screenshots/camera.png) | ![Profile](screenshots/profile.png) | ![Discover](screenshots/discover.png) |

## 🌍 Localization

Currently supports:
- **English** (Primary)
- **Pidgin English** (Nigerian Pidgin)
- **Hausa** (Northern Nigeria)
- **Yoruba** (Western Nigeria)
- **Igbo** (Eastern Nigeria)

## 🚀 Deployment

### **Debug Build**
```bash
./gradlew assembleDebug
```

### **Release Build**
```bash
./gradlew assembleRelease
```

### **Play Store Release**
1. Generate signed APK/AAB
2. Update version code and name
3. Add release notes
4. Upload to Google Play Console

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### **How to Contribute**
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Lead Developer**: [Your Name]
- **UI/UX Designer**: [Designer Name]
- **Backend Developer**: [Backend Dev]
- **QA Engineer**: [QA Name]

## 📞 Contact

- **Email**: contact@9jachat.com
- **Website**: https://9jachat.com
- **Twitter**: [@9jachat](https://twitter.com/9jachat)
- **Instagram**: [@9jachat](https://instagram.com/9jachat)

## 🙏 Acknowledgments

- Nigerian creative community for inspiration
- Open source libraries and their maintainers
- Android development community
- Material Design team at Google

---

**Made with ❤️ in Nigeria 🇳🇬**

*Bringing Nigerian creativity to the world, one video at a time.*