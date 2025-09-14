# LiveTV Android TV App

A modern Android TV app for streaming live sports with a focus on football matches. Features a clean Material Design interface with configurable sources and multiple stream format support.

![Build Status](https://github.com/FraPorta/LiveTVAndroidApp/workflows/Build%20and%20Release%20Android%20APK/badge.svg)

## 📱 Download & Install

### Quick Download
- **Latest Release**: [Download APK](https://github.com/FraPorta/LiveTVAndroidApp/releases/latest)
- **All Versions**: [Releases Page](https://github.com/FraPorta/LiveTVAndroidApp/releases)

### Installation
1. Download the latest APK from releases
2. Enable "Unknown Sources" in your Android settings
3. Install the APK on your device
4. Enjoy live sports streaming!

## ✨ Features

- **🏈 Football Focus**: Football matches as the default section
- **🎨 Modern UI**: Material Design 3 with responsive layout
- **⚡ Performance**: Pagination for faster loading
- **🔄 Refresh**: Individual match refresh functionality  
- **🌐 Multi-Source**: Acestream, M3U8, RTMP, and web streams
- **⚙️ Configurable**: Customizable scraping source URLs
- **📱 Responsive**: Optimized for TV, tablet, and phone screens
- **🔗 Intent Chooser**: Native Android app selection for streams

## 🚀 Quick Start

### For Users
1. Download the APK from [releases](https://github.com/FraPorta/LiveTVAndroidApp/releases)
2. Install on your Android TV/device
3. Launch and enjoy live sports!

### For Developers
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run

## 🛠️ Development

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17+
- Android SDK 24+ (Android 7.0)

### Build Process
```bash
# Clone repository
git clone https://github.com/FraPorta/LiveTVAndroidApp.git
cd LiveTVAndroidApp

# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease
```

### Creating Releases
```bash
# Use the release script
./release.sh 1.2.0 "New features and improvements"

# Or manually create tag
git tag v1.2.0
git push origin v1.2.0
```

## 🔄 Automated Deployment

This project uses GitHub Actions for automated builds and releases:

- **Push to `master`**: Automatic build and release
- **Tagged releases**: Formal version releases (e.g., `v1.0.0`)
- **Pull requests**: Build verification
- **Manual triggers**: On-demand builds

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment information.

## 📋 Project Structure

```
├── app/
│   ├── src/main/java/com/example/livetv/
│   │   ├── ui/           # UI components (Compose)
│   │   ├── data/         # Data layer (repositories, network)
│   │   └── model/        # Data models
├── .github/
│   ├── workflows/        # GitHub Actions workflows
│   └── ISSUE_TEMPLATE/   # Issue templates
├── release.sh           # Release automation script
└── DEPLOYMENT.md        # Deployment guide
```

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Add tests if applicable
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Open a Pull Request

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## ⚠️ Disclaimer

This app is for educational purposes only. The content is sourced from third-party websites. The developers are not responsible for the content and do not host any streams. Please ensure you have the legal right to view content in your jurisdiction.
