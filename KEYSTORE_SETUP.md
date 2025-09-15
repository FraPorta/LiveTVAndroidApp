# Release Keystore Setup Guide

## Creating a Release Keystore with Android Studio

### Step 1: Open Android Studio
1. Open your LiveTV project in Android Studio
2. Make sure the project has synced successfully

### Step 2: Generate Keystore
1. Go to **Build** → **Generate Signed Bundle / APK...**
2. Select **APK** and click **Next**
3. Click **Create new...** next to the Key store path

### Step 3: Fill Keystore Details
**Keystore Information:**
- **Key store path**: Browse to your project and select `app/keystore/release.keystore`
- **Password**: Choose a strong password (minimum 6 characters)
- **Confirm**: Re-enter the same password

**Key Information:**
- **Alias**: `livetv_release`
- **Key password**: Can be same as store password or different
- **Validity (years)**: `25` (recommended)

**Certificate Information:**
- **First and Last Name**: `LiveTV App` (or your name)
- **Organizational Unit**: `Development` (optional)
- **Organization**: `Your Organization` (optional)
- **City or Locality**: Your city
- **State or Province**: Your state/province
- **Country Code (XX)**: Your 2-letter country code (e.g., `US`, `IT`, `CA`)

### Step 4: Create and Save
1. Click **OK** to create the keystore
2. Android Studio will generate `release.keystore` in the `app/keystore/` folder
3. **Important**: Remember your passwords! Write them down securely.

### Step 5: Configure Project
1. Open `gradle.properties` in your project root
2. Uncomment and fill in the keystore configuration:

```properties
RELEASE_STORE_FILE=app/keystore/release.keystore
RELEASE_STORE_PASSWORD=your_actual_store_password
RELEASE_KEY_ALIAS=livetv_release
RELEASE_KEY_PASSWORD=your_actual_key_password
```

### Step 6: Test the Configuration
Build a release APK to test:
```bash
./gradlew assembleRelease
```

The APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

### Step 7: Verify Signing
Check that your APK is properly signed:
```bash
# On Windows (if Android SDK is in PATH)
# keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk

# On macOS/Linux
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
```

## Security Notes

### DO NOT:
- ❌ Commit the keystore file to git
- ❌ Share your keystore passwords
- ❌ Store passwords in plain text in shared locations

### DO:
- ✅ Keep the keystore file backed up securely
- ✅ Store passwords in a password manager
- ✅ Keep the keystore file in `app/keystore/` (already gitignored)

## For GitHub Actions (CI/CD)

After creating your keystore locally, use the setup script to prepare GitHub Secrets:

1. **Run the setup script:**
   ```bash
   ./setup-github-secrets.sh
   ```

2. **Copy the output and add to GitHub Secrets:**
   - Go to your repository on GitHub
   - Navigate to: Settings → Secrets and variables → Actions  
   - Add each Repository Secret with the values from the script output

3. **Required GitHub Secrets:**
   - `KEYSTORE_B64`: Base64 encoded keystore file
   - `RELEASE_STORE_PASSWORD`: Your keystore password
   - `RELEASE_KEY_ALIAS`: Your key alias
   - `RELEASE_KEY_PASSWORD`: Your key password

4. **Security Notes:**
   - ✅ Passwords are only in GitHub Secrets (encrypted)
   - ✅ Local development uses `local.properties` (not committed)
   - ✅ `gradle.properties` has no passwords (safe to commit)

5. **The CI workflow will automatically:**
   - Decode the keystore from secrets
   - Set up signing configuration
   - Build properly signed release APKs

## Troubleshooting

**Issue: "Keystore file not found"**
- Make sure the file path is correct: `app/keystore/release.keystore`
- Check that the keystore directory exists

**Issue: "Invalid keystore format"**
- Make sure you created a JKS or PKCS12 format keystore
- Try recreating the keystore with Android Studio

**Issue: "Wrong password"**
- Double-check your password in `gradle.properties`
- Make sure there are no extra spaces or characters

## Migration from Debug Keystore

⚠️ **Important**: Apps signed with different keystores cannot update each other. 

If you have the app installed with a debug keystore, users will need to:
1. Uninstall the current app (data will be lost)
2. Install the new release-signed version

Plan this migration carefully and communicate with your users.
