#!/bin/bash

# Version Release Script for LiveTV Android App
# Usage: ./release.sh [version] [message]
# Example: ./release.sh 1.2.0 "Added new features and bug fixes"

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "This script must be run from within a git repository"
    exit 1
fi

# Get version from parameter or prompt
if [ -z "$1" ]; then
    echo -n "Enter version number (e.g., 1.0.0): "
    read VERSION
else
    VERSION=$1
fi

# Validate version format
if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    print_error "Version must be in format X.Y.Z (e.g., 1.0.0)"
    exit 1
fi

# Get commit message from parameter or prompt
if [ -z "$2" ]; then
    echo -n "Enter release message (optional): "
    read MESSAGE
else
    MESSAGE=$2
fi

print_info "Creating release for version $VERSION"

# Check if tag already exists
if git tag -l | grep -q "^v$VERSION$"; then
    print_error "Tag v$VERSION already exists"
    exit 1
fi

# Check if there are uncommitted changes
if ! git diff --quiet || ! git diff --cached --quiet; then
    print_warning "There are uncommitted changes"
    echo -n "Do you want to commit them first? (y/N): "
    read COMMIT_CHANGES
    
    if [ "$COMMIT_CHANGES" = "y" ] || [ "$COMMIT_CHANGES" = "Y" ]; then
        git add -A
        git commit -m "Prepare release v$VERSION"
        print_success "Changes committed"
    else
        print_error "Please commit or stash your changes before creating a release"
        exit 1
    fi
fi

# Update version in build.gradle.kts if it exists
if [ -f "app/build.gradle.kts" ]; then
    print_info "Updating version in app/build.gradle.kts"
    
    # Extract current version name and code
    CURRENT_VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | sed -E 's/.*versionName\s*=\s*"([^"]+)".*/\1/' || echo "")
    CURRENT_VERSION_CODE=$(grep -E 'versionCode\s*=' app/build.gradle.kts | sed -E 's/.*versionCode\s*=\s*([0-9]+).*/\1/' || echo "1")
    
    # Increment version code
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    
    # Update version name and code
    sed -i.bak "s/versionName = \".*\"/versionName = \"$VERSION\"/" app/build.gradle.kts
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
    
    # Remove backup file
    rm -f app/build.gradle.kts.bak
    
    print_success "Updated version to $VERSION (code: $NEW_VERSION_CODE)"
    
    # Commit version changes
    git add app/build.gradle.kts
    git commit -m "Bump version to $VERSION"
fi

# Create and push tag
print_info "Creating tag v$VERSION"
if [ -n "$MESSAGE" ]; then
    git tag -a "v$VERSION" -m "Release v$VERSION: $MESSAGE"
else
    git tag -a "v$VERSION" -m "Release v$VERSION"
fi

print_info "Pushing changes and tag to remote"
git push origin $(git branch --show-current)
git push origin "v$VERSION"

print_success "Release v$VERSION created successfully!"
print_info "GitHub Actions will automatically build and create the release"
print_info "You can monitor the progress at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/actions"
print_info "The release will be available at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:/]\([^.]*\).*/\1/')/releases"

echo
print_info "Next steps:"
echo "1. Wait for GitHub Actions to complete the build"
echo "2. Check the release page for the APK download"
echo "3. Test the new release on your devices"
echo "4. Update any documentation if needed"
