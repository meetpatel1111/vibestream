# VibeStream — Cross-Platform Video & Music Player

A modern, offline-first media player inspired by VLC and MX Player, built with Kotlin and Jetpack Compose for Android.

## 🎯 Current Status (M1 Milestone - 70% Complete)

### ✅ Completed Features

1. **Project Foundation**
   - ✅ Android project setup with Kotlin & Compose
   - ✅ Media3/ExoPlayer integration
   - ✅ Hilt dependency injection
   - ✅ Room database configuration

2. **Core Architecture**
   - ✅ Player interface abstraction
   - ✅ Library interface design
   - ✅ Comprehensive data models
   - ✅ ExoPlayer implementation
   - ✅ Playback controller with queue management

3. **Database Schema**
   - ✅ Media items with full metadata
   - ✅ Playlists and playlist items
   - ✅ Play history tracking
   - ✅ Subtitle tracks
   - ✅ Audio device profiles
   - ✅ Settings storage
   - ✅ Room DAOs for all entities

4. **Gesture Control System**
   - ✅ MX-style gesture detection
   - ✅ Horizontal swipe for seek
   - ✅ Vertical swipe (left: brightness, right: volume)
   - ✅ Double-tap for skip forward/backward
   - ✅ Gesture feedback overlays
   - ✅ System brightness/volume control

5. **UI Foundation**
   - ✅ Material Design 3 theme
   - ✅ Navigation between screens
   - ✅ Now Playing screen with gesture integration
   - ✅ Playback controls UI
   - ✅ Video surface placeholder

### 🚧 In Progress

- **Library Scanner**: Media file scanning with metadata extraction
- **Background Playback**: Foreground service implementation
- **Subtitle System**: External subtitle loading and sync

### 📋 Next Steps (Remaining M1 Tasks)

1. **Subtitle System**
   - External subtitle file loading (.srt, .ass, .vtt)
   - Subtitle styling and positioning
   - Sync adjustment controls

2. **Audio DSP Features**
   - 10-band equalizer
   - Bass boost and virtualizer
   - Playback speed with tempo preservation
   - ReplayGain support

3. **Library Scanner**
   - File system scanning
   - Metadata extraction with FFmpeg
   - Thumbnail generation
   - Waveform analysis

4. **Background Playback**
   - Media session service
   - Notification controls
   - Picture-in-Picture support

5. **Playlist Management**
   - Queue operations
   - Smart playlists
   - Playlist persistence

## 🏗️ Architecture Overview

```\n┌─────────────── UI Layer (Compose) ──────────────┐\n│ Home │ Videos │ Music │ NowPlaying │ Settings │\n└─────────────────┬───────────────────────────────┘\n                  │\n        ┌─────────┴─────────┐\n        │ ViewModels (Hilt) │\n        └─────────┬─────────┘\n                  │\n   ┌──────────────┴──────────────┐\n   │     Domain Layer            │\n   │ ┌─────────┬─────────────────┤\n   │ │ Player  │ PlaybackController│\n   │ │ Library │ GestureDetector │\n   │ └─────────┴─────────────────│\n   └──────────────┬──────────────┘\n                  │\n   ┌──────────────┴──────────────┐\n   │     Data Layer              │\n   │ ┌─────────┬─────────────────┤\n   │ │ExoPlayer│ Room Database   │\n   │ │Media3   │ Settings Store  │\n   │ │Scanner  │ File System     │\n   │ └─────────┴─────────────────│\n   └─────────────────────────────┘\n```\n\n## 🔧 Tech Stack\n\n- **Language**: Kotlin\n- **UI Framework**: Jetpack Compose + Material Design 3\n- **Media Engine**: Media3/ExoPlayer\n- **Database**: Room + SQLite\n- **DI**: Hilt\n- **Navigation**: Navigation Compose\n- **State Management**: StateFlow + Compose State\n- **Architecture**: MVVM + Clean Architecture\n\n## 📱 Key Features Implemented\n\n### Gesture Controls (MX-Style)\n- **Single tap**: Show/hide controls\n- **Double-tap left/right**: Skip backward/forward (10s)\n- **Horizontal swipe**: Seek with preview\n- **Vertical swipe left**: Screen brightness\n- **Vertical swipe right**: Volume control\n- **Visual feedback**: HUD overlays for all gestures\n\n### Media Support\n- **Containers**: MP4, MKV, WebM, AVI, MOV, TS, MPG\n- **Video Codecs**: H.264, H.265, VP8, VP9, AV1, MPEG-2\n- **Audio Codecs**: AAC, MP3, Opus, Vorbis, FLAC, AC-3\n- **Streaming**: HLS, DASH, Progressive HTTP\n\n### Database Schema\n- **Media Items**: Comprehensive metadata storage\n- **Playlists**: Custom and smart playlists\n- **Play History**: Resume points and statistics\n- **Settings**: Persistent app configuration\n- **Audio Profiles**: EQ presets per device\n\n## 🚀 Getting Started\n\n### Prerequisites\n- Android Studio Iguana | 2023.2.1 or later\n- Android SDK API 26+ (Android 8.0)\n- Kotlin 1.9.20+\n\n### Building\n```bash\ngit clone <repository-url>\ncd VibeStream\n./gradlew assembleDebug\n```\n\n### Running\n```bash\n./gradlew installDebug\n```\n\n## 📁 Project Structure\n\n```\napp/src/main/java/com/vibestream/player/\n├── data/\n│   ├── database/          # Room entities, DAOs, database\n│   ├── model/             # Data models and DTOs\n│   └── player/            # ExoPlayer implementation\n├── domain/\n│   ├── library/           # Library interface\n│   └── player/            # Player interface and controller\n├── service/               # Background services\n├── ui/\n│   ├── gesture/           # Gesture detection and feedback\n│   ├── navigation/        # Navigation configuration\n│   ├── screen/            # Screen composables\n│   └── theme/             # Material Design theme\n├── util/                  # Utility classes\n└── di/                    # Hilt dependency injection\n```\n\n## 🎨 Design Principles\n\n1. **Offline-First**: Core functionality works without internet\n2. **Performance**: Hardware acceleration, zero-copy pipelines\n3. **Privacy**: No telemetry by default, local-only analytics\n4. **Accessibility**: Screen reader support, large touch targets\n5. **Battery Efficiency**: Adaptive quality, background optimization\n\n## 🔮 Roadmap\n\n### M2 (Next Release)\n- ✅ Complete M1 remaining features\n- 🎯 Chromecast/AirPlay support\n- 🎯 SMB/UPnP network browsing\n- 🎯 Advanced video filters\n- 🎯 Waveform seekbar\n\n### M3 (Future)\n- 🎯 Desktop builds (Windows, macOS, Linux)\n- 🎯 Web PWA version\n- 🎯 Cloud connectors\n- 🎯 Pro features\n\n## 📄 License\n\nTo be determined - considering GPL v3 or Apache 2.0\n\n## 🤝 Contributing\n\nContributions welcome! Please read the contributing guidelines first.\n\n---\n\n**VibeStream** - *Play everything, everywhere* 🎵📹