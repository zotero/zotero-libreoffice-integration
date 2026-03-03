#!/bin/bash
#
# Build Zotero.jar and package the .oxt extension.
#
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$SCRIPT_DIR/.."
BUILD_DIR="$PROJECT_DIR/build"

. "$SCRIPT_DIR/config.sh"

SOURCE_DIR="$BUILD_DIR/source"
BIN_DIR="$BUILD_DIR/bin"
LIB_DIR="$BUILD_DIR/lib"
OXT_DIR="$BUILD_DIR/oxt"
MANIFEST="$BUILD_DIR/META-INF/MANIFEST.MF"
INSTALL_DIR="$PROJECT_DIR/install"

# ---------------------------------------------------------------------------
# 1. Build compile-time classpath from lib/ jars
# ---------------------------------------------------------------------------
CLASSPATH=""
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
done
if [ -d "$LIB_DIR/libreoffice-sdk" ]; then
    for jar in "$LIB_DIR/libreoffice-sdk"/*.jar; do
        [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
    done
fi
CLASSPATH="${CLASSPATH#:}"

# ---------------------------------------------------------------------------
# 2. Compile Java sources and copy resources → build/bin
# ---------------------------------------------------------------------------
echo "Compiling sources..."
rm -rf "$BIN_DIR"
mkdir -p "$BIN_DIR"

JAVA_FILES=()
while IFS= read -r -d '' f; do
    JAVA_FILES+=("$f")
done < <(find "$SOURCE_DIR" -name '*.java' -print0)

if [ ${#JAVA_FILES[@]} -eq 0 ]; then
    echo "ERROR: No .java files found in $SOURCE_DIR" >&2
    exit 1
fi

javac \
    -source 8 -target 8 \
    -cp "$CLASSPATH" \
    -d "$BIN_DIR" \
    "${JAVA_FILES[@]}"

echo "Compiled ${#JAVA_FILES[@]} source files."

find "$SOURCE_DIR" -type f ! -name '*.java' -exec sh -c '
    for f; do
        rel="${f#'"$SOURCE_DIR"'/}"
        mkdir -p "'"$BIN_DIR"'/$(dirname "$rel")"
        cp "$f" "'"$BIN_DIR"'/$rel"
    done
' _ {} +

# ---------------------------------------------------------------------------
# 3. Create Zotero.jar in build/oxt
# ---------------------------------------------------------------------------
echo "Building Zotero.jar..."
jar cfm "$OXT_DIR/Zotero.jar" "$MANIFEST" -C "$BIN_DIR" .

# ---------------------------------------------------------------------------
# 4. Package .oxt
# ---------------------------------------------------------------------------
echo "Packaging .oxt..."

# Copy runtime jars into oxt
mkdir -p "$OXT_DIR/external_jars"
cp "$LIB_DIR"/*jar "$OXT_DIR/external_jars/"

# Zip up oxt
mkdir -p "$INSTALL_DIR"
rm -f "$INSTALL_DIR/Zotero_LibreOffice_Integration.oxt"
(
    cd "$OXT_DIR"
    zip -r "$INSTALL_DIR/Zotero_LibreOffice_Integration.oxt" . \
        -x '*/.svn/*' -x '*/.DS_Store'
)

# Clean up
rm -rf "$OXT_DIR/Zotero.jar" "$OXT_DIR/external_jars"

echo ""
echo "Done → install/Zotero_LibreOffice_Integration.oxt"
