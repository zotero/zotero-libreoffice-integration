#!/bin/bash

set -e

usage() {
	echo "Usage: $0 <version>"
	echo ""
	echo "Examples:"
	echo "  $0 7.0.5        # Set version to 7.0.5"
	echo "  $0 7.1.0        # Set version to 7.1.0"
	echo ""
	echo "This script updates version in:"
	echo "  - resource/version.txt (as VERSION.SOURCE)"
	echo "  - resource/installer.mjs (LAST_INSTALLED_FILE_UPDATE as VERSIONpre)"
	echo "  - build/oxt/description.xml (version attribute)"
	exit 1
}

validate_version() {
	local version="$1"
	if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
		echo "Error: Invalid version format: $version"
		echo "Error: Version must be in format X.Y.Z (e.g., 7.0.5)"
		exit 1
	fi
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

VERSION_FILE="$PROJECT_ROOT/resource/version.txt"
INSTALLER_FILE="$PROJECT_ROOT/resource/installer.mjs"
DESCRIPTION_FILE="$PROJECT_ROOT/build/oxt/description.xml"

if [ $# -ne 1 ]; then
	echo "Error: Version argument is required"
	usage
fi

NEW_VERSION="$1"

validate_version "$NEW_VERSION"

echo "Bumping version to $NEW_VERSION..."

echo "Updating $VERSION_FILE"
echo "${NEW_VERSION}.SOURCE" > "$VERSION_FILE"

echo "Updating $INSTALLER_FILE"
sed -i "s/this\.LAST_INSTALLED_FILE_UPDATE = \"[^\"]*\";/this.LAST_INSTALLED_FILE_UPDATE = \"${NEW_VERSION}pre\";/" "$INSTALLER_FILE"

echo "Updating $DESCRIPTION_FILE"
sed -i "s/<version value=\"[^\"]*\"\/>/<version value=\"${NEW_VERSION}\"\/>/" "$DESCRIPTION_FILE"

echo "Version successfully bumped to $NEW_VERSION"
