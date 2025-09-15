#!/bin/bash

# Script to prepare GitHub Secrets for Android keystore

echo "🔐 GitHub Secrets Setup for Android Keystore"
echo "=============================================="
echo

# Check if keystore exists
KEYSTORE_FILE="app/keystore/release.keystore.jks"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ Keystore file not found at: $KEYSTORE_FILE"
    echo "Please create the keystore first using Android Studio"
    exit 1
fi

echo "✅ Found keystore file: $KEYSTORE_FILE"
echo

# Encode keystore to base64
echo "📦 Encoding keystore to base64..."
KEYSTORE_B64=$(base64 -w 0 "$KEYSTORE_FILE")

echo "🔑 GitHub Secrets to add:"
echo "========================="
echo
echo "Go to your GitHub repository → Settings → Secrets and variables → Actions"
echo "Add these Repository Secrets:"
echo
echo "Secret Name: KEYSTORE_B64"
echo "Secret Value: $KEYSTORE_B64"
echo
# Read values from keystore.properties
if [ -f "keystore.properties" ]; then
    STORE_PASSWORD=$(grep "RELEASE_STORE_PASSWORD" keystore.properties | cut -d'=' -f2)
    KEY_ALIAS=$(grep "RELEASE_KEY_ALIAS" keystore.properties | cut -d'=' -f2)
    KEY_PASSWORD=$(grep "RELEASE_KEY_PASSWORD" keystore.properties | cut -d'=' -f2)
else
    echo "❌ keystore.properties not found!"
    exit 1
fi

echo "Secret Name: RELEASE_STORE_PASSWORD"
echo "Secret Value: $STORE_PASSWORD"
echo
echo "Secret Name: RELEASE_KEY_ALIAS" 
echo "Secret Value: $KEY_ALIAS"
echo
echo "Secret Name: RELEASE_KEY_PASSWORD"
echo "Secret Value: $KEY_PASSWORD"
echo
echo "📋 Copy the values above and add them as secrets in GitHub"
echo
echo "🎯 After adding secrets, push your changes to trigger a release build!"
echo

# Test local build
echo "🧪 Testing local release build..."
if ./gradlew assembleRelease --quiet; then
    echo "✅ Local release build successful!"
    echo "📱 APK generated at: app/build/outputs/apk/release/app-release.apk"
else
    echo "❌ Local release build failed. Check your keystore configuration."
    exit 1
fi
