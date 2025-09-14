# LiveTV Android App - Deployment Guide

## Automated Build and Release Pipeline

This repository is configured with GitHub Actions to automatically build and release APK files. Here's how it works:

## 🚀 Getting the Latest Version

### Method 1: Download from Releases (Recommended)
1. Go to the [Releases page](https://github.com/FraPorta/LiveTVAndroidApp/releases)
2. Download the latest `.apk` file
3. Install on your Android device/TV

### Method 2: Download from Actions Artifacts
1. Go to the [Actions tab](https://github.com/FraPorta/LiveTVAndroidApp/actions)
2. Click on the latest successful workflow run
3. Download the APK from the "Artifacts" section
4. Extract and install the APK

## 📋 Release Types

### Automatic Releases
- **Push to `master`**: Creates automatic releases with version info
- **Tagged releases**: Create a tag like `v1.0.0` to trigger a formal release

### Manual Releases
- Go to Actions tab → "Build and Release Android APK" → "Run workflow"
- Choose between `debug` or `release` build

## 🏷️ Creating a Tagged Release

To create a new version release:

```bash
# Create and push a new tag
git tag v1.0.0
git push origin v1.0.0
```

This will automatically:
1. Build the release APK
2. Create a GitHub release
3. Upload the APK file
4. Generate release notes

## 🔧 Pipeline Configuration

### Workflows
- **`build-and-release.yml`**: Main workflow for continuous deployment
- **`release-on-tag.yml`**: Simple workflow for tagged releases

### Build Process
1. **Setup**: Java 17, Gradle caching
2. **Build**: Clean → Assemble APK
3. **Version**: Extract version info from build files
4. **Release**: Create GitHub release with APK
5. **Artifacts**: Upload APK files with retention

### APK Naming Convention
```
LiveTV-v{version}-{date}-{commit}-{type}.apk
```
Example: `LiveTV-v1.0.0-20240914-a1b2c3d-release.apk`

## 📱 Installation Instructions

### For Android TV
1. Enable "Unknown Sources":
   - Settings → Device Preferences → Security & restrictions → Unknown sources
   - Enable for your file manager app
2. Transfer APK via USB or network
3. Install using a file manager

### For Android Phone/Tablet
1. Enable "Unknown Sources":
   - Settings → Security → Unknown sources (Android < 8)
   - Settings → Apps → Special access → Install unknown apps (Android 8+)
2. Download APK directly or transfer from computer
3. Tap the APK file to install

## 🔒 Security Notes

- APK files are unsigned (for development)
- You may see security warnings - this is normal for sideloaded apps
- Only download APKs from the official repository releases

## 🛠️ Development Workflow

### For Contributors
1. Create feature branch: `git checkout -b feature/new-feature`
2. Make changes and test locally
3. Create pull request to `master`
4. After merge, automatic build triggers

### For Maintainers
1. Review and merge pull requests
2. Automatic release is created
3. For major versions, create tagged release:
   ```bash
   git tag v2.0.0
   git push origin v2.0.0
   ```

## 📊 Build Status

Current build status: ![Build Status](https://github.com/FraPorta/LiveTVAndroidApp/workflows/Build%20and%20Release%20Android%20APK/badge.svg)

## 🐛 Troubleshooting

### Build Fails
- Check the Actions logs for detailed error messages
- Ensure all dependencies are properly configured
- Verify `gradlew` has execute permissions

### Installation Issues
- Verify "Unknown Sources" is enabled
- Check available storage space
- Try restarting the device after installation

### App Crashes
- Check if the APK is compatible with your Android version
- Try clearing app data: Settings → Apps → LiveTV → Storage → Clear Data

## 📝 Version History

All releases are documented in the [Releases page](https://github.com/FraPorta/LiveTVAndroidApp/releases) with:
- Version number and build info
- Feature changes and improvements
- Installation instructions
- Download links for APK files

---

## 🤝 Contributing

To contribute to this project:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Create a pull request

The pipeline will automatically build and test your changes!
