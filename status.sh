#!/bin/bash

# Deployment Status Check Script
# This script checks the status of the latest deployment

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo "🚀 LiveTV Android App - Deployment Status"
echo "========================================"

# Check if we're in the right repository
if ! git remote get-url origin | grep -q "LiveTVAndroidApp"; then
    print_warning "This doesn't appear to be the LiveTV repository"
fi

# Get repository info
REPO_URL=$(git remote get-url origin | sed 's/git@github.com:/https:\/\/github.com\//' | sed 's/\.git$//')
REPO_NAME=$(echo $REPO_URL | sed 's/.*github.com\///')

print_info "Repository: $REPO_NAME"

# Get latest tag
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "No tags found")
print_info "Latest tag: $LATEST_TAG"

# Get current branch and commit
CURRENT_BRANCH=$(git branch --show-current)
CURRENT_COMMIT=$(git rev-parse --short HEAD)
print_info "Current branch: $CURRENT_BRANCH"
print_info "Current commit: $CURRENT_COMMIT"

# Check if there are uncommitted changes
if ! git diff --quiet || ! git diff --cached --quiet; then
    print_warning "There are uncommitted changes"
else
    print_success "Working directory is clean"
fi

echo
print_info "🔗 Useful Links:"
echo "   📋 Actions: $REPO_URL/actions"
echo "   🎯 Releases: $REPO_URL/releases"
echo "   📥 Latest APK: $REPO_URL/releases/latest"
echo "   🐛 Issues: $REPO_URL/issues"

echo
print_info "📱 Installation Instructions:"
echo "   1. Download APK from releases page"
echo "   2. Enable 'Unknown Sources' in Android settings"
echo "   3. Install APK on your device"

echo
print_info "🛠️  Development Commands:"
echo "   Build debug:   ./gradlew assembleDebug"
echo "   Build release: ./gradlew assembleRelease"
echo "   Create tag:    git tag v1.0.0 && git push origin v1.0.0"
echo "   New release:   ./release.sh 1.0.0 'Release notes'"

# Check for newer versions on GitHub (if curl is available)
if command -v curl >/dev/null 2>&1; then
    print_info "Checking for latest release on GitHub..."
    GITHUB_LATEST=$(curl -s "https://api.github.com/repos/$REPO_NAME/releases/latest" | grep '"tag_name":' | sed -E 's/.*"tag_name": "([^"]+)".*/\1/' 2>/dev/null || echo "Unable to fetch")
    
    if [ "$GITHUB_LATEST" != "Unable to fetch" ] && [ "$GITHUB_LATEST" != "$LATEST_TAG" ]; then
        print_warning "GitHub has a newer release: $GITHUB_LATEST (local: $LATEST_TAG)"
        print_info "Consider pulling the latest changes: git pull origin master"
    elif [ "$GITHUB_LATEST" = "$LATEST_TAG" ]; then
        print_success "Local repository is up to date with GitHub"
    fi
fi

echo
print_success "Status check complete!"
