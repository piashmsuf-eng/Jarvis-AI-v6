# 🤖 Jarvis AI v7.0

**The Ultimate Android AI Assistant** - Bengali + English | Voice-Controlled | Root-Capable | Automation-Powered

[![Android CI](https://github.com/piashmsuf-eng/Jarvis-AI-v6/actions/workflows/android.yml/badge.svg)](https://github.com/piashmsuf-eng/Jarvis-AI-v6/actions)
[![Version](https://img.shields.io/badge/version-7.0.0-blue.svg)](https://github.com/piashmsuf-eng/Jarvis-AI-v6)
[![API](https://img.shields.io/badge/API-28%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=28)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)

---

## 🎯 Overview

Jarvis AI is an advanced, voice-controlled Android assistant inspired by Tony Stark's J.A.R.V.I.S. from the MCU. Built for power users who want complete control over their device through natural language commands in **Bengali** and **English**.

### 🌟 What Makes It Special

- **🇧🇩 Bilingual**: Full Bengali and English support (200+ translations)
- **🎤 Voice-First**: "Hey Jarvis" wake word detection
- **📱 Deep Integration**: SMS, Calls, WhatsApp, Files, Apps
- **🎭 Personality Modes**: 5 distinct AI personalities
- **👁️ Vision Capable**: OCR, screenshot analysis, screen reading
- **🏠 Smart Home Ready**: HTTP/MQTT device control
- **🤖 Automation**: 6 smart routines for common scenarios
- **🔧 Root Support**: Enhanced capabilities with LibSU

---

## ✨ Features (v7.0)

### 📞 Communication
- **SMS Management**: Send, read, organize messages
- **Call Control**: Make calls, view history, end calls (Android 9+)
- **WhatsApp**: Send messages via accessibility service
- **Auto-Announcements**: Voice notifications for incoming SMS/calls

### 📂 File & App Management
- **File Organization**: Auto-organize downloads by type
- **Cache Cleaning**: Clear temp files and app caches
- **App Control**: Launch, kill, and manage apps
- **Recent Downloads**: Quick access to downloaded files

### 🎭 Personality Engine
Choose from 5 distinct personalities:
1. **Professional** - "Good day, Sir. How may I assist you?"
2. **Casual** - "Hey Boss! What's up?"
3. **Funny** - "Well, well, well... If it isn't my favorite human."
4. **Romantic** - "Greetings, dear one. Your presence brings light."
5. **Jarvis Movie** - "Welcome home, Sir." (MCU-style)

### 👁️ Vision Capabilities
- **OCR**: Extract text from images using ML Kit
- **Screenshot**: Capture screen via accessibility or root
- **Screen Analysis**: Detect apps, buttons, and content
- **Smart Summarization**: Understand what's on screen

### 🏠 Smart Home
- **Device Control**: Lights, AC, TV, fans, and more
- **HTTP API**: REST-based device commands
- **MQTT Ready**: Pub/sub protocol support
- **Quick Actions**: "Turn on all lights", "Set AC to 24°"

### 🤖 Automation Routines
Pre-built automation sequences:
1. **Morning Routine** - DND off, news, calendar
2. **Night Routine** - DND on, close apps, clean cache
3. **Workout Mode** - Priority DND, music player
4. **Focus Mode** - Alarms only, close social media
5. **Driving Mode** - Priority DND, maps, music
6. **Cleanup** - Cache clear, organize files

---

## 🎤 Voice Commands

### Bengali Commands (বাংলা)
```
"মা কে এসএমএস করো"          → Send SMS to mom
"বাবা কে কল করো"            → Call dad
"শেষ ৫টি মেসেজ পড়"          → Read last 5 messages
"হোয়াটসঅ্যাপ মেসেজ"        → WhatsApp message
"ইউটিউব খোলো"              → Open YouTube
"গান বাজাও"                 → Play music
"স্ক্রিনশট নাও"             → Take screenshot
"লাইট জ্বালাও"             → Turn on lights
"মর্নিং রুটিন"              → Morning routine
"ফানি মোড"                  → Funny mode
```

### English Commands
```
"Send SMS to mom"            → Send text message
"Call dad"                   → Make phone call
"Read last 5 messages"       → Read recent SMS
"Open YouTube"               → Launch app
"Play music on Spotify"      → Open music app
"Take screenshot"            → Capture screen
"What's on screen?"          → Analyze display
"Turn on lights"             → Smart home control
"Morning routine"            → Run automation
"Switch to professional mode" → Change personality
```

---

## 📱 Installation

### Requirements
- Android 9.0 (API 28) or higher
- 100 MB free storage
- Root access (optional, for enhanced features)

### Download APK

#### Option 1: GitHub Actions (Recommended)
1. Go to [Actions](https://github.com/piashmsuf-eng/Jarvis-AI-v6/actions)
2. Click latest successful build (green ✅)
3. Scroll to **Artifacts** section
4. Download **jarvis-ai-debug-apk**
5. Extract ZIP and install APK

#### Option 2: Releases
Download latest APK from [Releases](https://github.com/piashmsuf-eng/Jarvis-AI-v6/releases)

### Setup Steps

1. **Install APK**
   ```
   Settings → Security → Enable "Unknown Sources"
   Open jarvis-ai-debug.apk → Install
   ```

2. **Grant Permissions**
   - Microphone (voice input)
   - Contacts (call/SMS features)
   - Phone (call management)
   - SMS (message automation)
   - Storage (file management)
   - Camera (vision features)
   - Notification Access (listener service)
   - Accessibility Service (WhatsApp, screen reading)
   - Display over other apps (floating UI)

3. **Configure API Keys**
   ```
   Settings → AI Configuration
   - Cartesia TTS API Key (voice)
   - Picovoice Access Key (wake word)
   - OpenAI/Anthropic/Local LLM endpoint
   ```

4. **Enable Wake Word**
   ```
   Settings → Wake Word
   Enable "Hey Jarvis" detection
   ```

5. **Select Personality**
   ```
   Settings → Personality
   Choose: Professional / Casual / Funny / Romantic / Jarvis Movie
   ```

---

## 🏗️ Architecture

### Core Components

```
Jarvis AI v7.0
├── Foundation Layer
│   ├── Jetpack Compose (Material 3 UI)
│   ├── Room Database (conversation history)
│   ├── ML Kit (OCR & vision)
│   └── LibSU (root access)
│
├── Language Layer
│   ├── LanguageDetector (auto-detect Bengali/English)
│   ├── BengaliCommandParser (200+ translations)
│   └── BengaliVoiceConfig (8 TTS voices)
│
├── Communication Layer
│   ├── ContactManager (lookup & search)
│   ├── SmsController (send/receive/read)
│   ├── CallController (make/end/history)
│   └── WhatsAppController (accessibility automation)
│
├── Automation Layer
│   ├── FileController (organize, clean, manage)
│   ├── AppController (launch, kill, cache)
│   └── RoutineManager (6 smart routines)
│
├── Intelligence Layer
│   ├── PersonalityEngine (5 modes)
│   ├── VisionAnalyzer (ML Kit OCR)
│   └── ScreenshotController (capture)
│
├── Smart Home Layer
│   └── SmartHomeController (HTTP/MQTT)
│
└── Core Brain
    └── JarvisBrain (43 actions, bilingual processing)
```

### Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material 3
- **Build**: Gradle 8.2 + KSP
- **Database**: Room 2.6.1
- **Networking**: OkHttp + Retrofit
- **Voice**: Picovoice Porcupine + Cartesia TTS
- **Vision**: ML Kit Text Recognition
- **Root**: LibSU 5.2.2
- **Animations**: Lottie Compose
- **Image Loading**: Coil

---

## 🛠️ Build Instructions

### Prerequisites
```bash
# Install Android Studio
# Download from: https://developer.android.com/studio

# Install Android SDK (API 34)
# Configure ANDROID_HOME environment variable

# Clone repository
git clone https://github.com/piashmsuf-eng/Jarvis-AI-v6.git
cd Jarvis-AI-v6
```

### Configure Secrets

Create `local.properties` in project root:
```properties
sdk.dir=/path/to/Android/sdk
PICOVOICE_ACCESS_KEY=your_key_from_picovoice.ai
CARTESIA_API_KEY=your_cartesia_api_key
```

### Build APK

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore)
./gradlew assembleRelease

# Output location
# app/build/outputs/apk/debug/app-debug.apk
```

### Run on Device

```bash
# Install on connected device
./gradlew installDebug

# Or use ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎨 Customization

### Theme Configuration

Jarvis AI includes 4 built-in themes:

1. **Jarvis Cyan** (Default) - Arc Reactor inspired
2. **Iron Man** - Red & Gold
3. **Hulk** - Green
4. **Thanos** - Purple

Change theme in: `Settings → Appearance → Theme`

### Bengali Voice Selection

Choose from 8 Bengali TTS voices:
- Male: Professional, Friendly, Formal, Casual
- Female: Professional, Friendly, Formal, Casual

Configure in: `Settings → Language → Bengali Voice`

### Smart Home Devices

Add devices in code:
```kotlin
val device = SmartDevice(
    id = "living_room_light",
    name = "Living Room Light",
    type = DeviceType.LIGHT,
    protocol = Protocol.HTTP,
    address = "192.168.1.100",
    port = 80
)
smartHomeController.registerDevice(device)
```

---

## 📊 Statistics

- **Total Files**: 57 Kotlin files
- **Lines of Code**: ~70,000
- **Voice Actions**: 43 commands
- **Languages**: 2 (Bengali + English)
- **Bengali Translations**: 200+
- **Personality Modes**: 5
- **Smart Routines**: 6
- **Device Types**: 10 (smart home)

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Add Bengali translations for new features
- Test on Android 9+ devices
- Update README for major changes
- Keep commits atomic and descriptive

---

## 🐛 Known Issues

- Screenshot capture requires Android 9+ or root access
- WhatsApp automation depends on accessibility service
- Call ending requires Android 9+ (API 28+)
- Some smart home features require local network access

---

## 🔮 Roadmap

### Phase 7: UI Screens (Coming Soon)
- [ ] Dashboard with stats & quick actions
- [ ] Conversation history screen
- [ ] Skills browser
- [ ] Personality settings UI

### Future Enhancements
- [ ] Custom wake word training
- [ ] Video editing automation
- [ ] Multi-device sync
- [ ] LLaMA local inference
- [ ] Gesture control
- [ ] Widget support

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Piash** - *Lead Developer* - [@piashmsuf-eng](https://github.com/piashmsuf-eng)
- **Letta Code** - *AI Development Assistant* - [letta.com](https://letta.com)

---

## 🙏 Acknowledgments

- Inspired by Marvel Cinematic Universe's J.A.R.V.I.S.
- Maya AI project for feature inspiration
- Picovoice for wake word detection
- Cartesia for high-quality TTS
- Google ML Kit for vision capabilities
- LibSU for root access framework

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/piashmsuf-eng/Jarvis-AI-v6/issues)
- **Discord**: [discord.gg/letta](https://discord.gg/letta)
- **Email**: piashmsuf.eng@gmail.com

---

## ⭐ Star History

If you find this project useful, please consider giving it a star! ⭐

---

**Made with ❤️ in Bangladesh** 🇧🇩

**Powered by [Letta Code](https://letta.com)** 👾
