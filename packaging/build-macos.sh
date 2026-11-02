#!/usr/bin/env bash
set -euo pipefail

# Ensure running on macOS
if [ "$(uname -s)" != "Darwin" ]; then
  echo "Error: This script must be run on macOS (detected: $(uname -s))." >&2
  exit 1
fi

_HOST_ARCH="$(uname -m)"
case "$_HOST_ARCH" in
  x86_64)
    ARCH="x86_64"
    ;;
  arm64|aarch64)
    ARCH="arm64"
    ;;
  *)
    echo "Unsupported architecture: $_HOST_ARCH" >&2
    exit 1
    ;;
esac

VERSION="${APP_VERSION:-1.0.0}"
VERSION="${VERSION//_/-}"
SRC_DIR="${SRC_DIR:-$(pwd)}"
OUT_DIR="${OUT_DIR:-${SRC_DIR}/dist}"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-macos-package"
APP_NAME="GeoGebra Discovery"
APP_BUNDLE="${PACKAGE_DIR}/${APP_NAME}.app"

echo ">>> Target version: ${VERSION} (${ARCH})"

# Ensure build tools and JDK 11 are present
if ! command -v brew >/dev/null 2>&1; then
  echo "Error: Homebrew is required to install dependencies." >&2
  exit 1
fi

if ! brew list temurin@11 >/dev/null 2>&1; then
  echo ">>> Installing Temurin OpenJDK 11..."
  brew install --cask temurin@11
fi

export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
export PATH="${JAVA_HOME}/bin:${PATH}"

# Shadow source files
echo ">>> Copying project source files..."
rm -rf "${GGD_SRC_SHADOW}"
mkdir -p "${GGD_SRC_SHADOW}"

for item in desktop gradle gradlew build.gradle init.gradle presettings.gradle \
            settings.gradle retex gradle-scripts carota-web common common-jre \
            jogl2 input3D giac-jni tarski-jni web ggbjdk keyboard-base \
            keyboard-scientific keyboard-web test xr-base; do
  if [ -e "${SRC_DIR}/${item}" ]; then
    cp -RP "${SRC_DIR}/${item}" "${GGD_SRC_SHADOW}/"
  fi
done

# Build project
echo ">>> Building Geogebra-discovery..."
cd "${GGD_SRC_SHADOW}"
./gradlew :desktop:installDist

# Generate a stripped-down bundled JRE using jlink
BUNDLED_RUNTIME="${PACKAGE_DIR}/runtime"
rm -rf "${BUNDLED_RUNTIME}"

echo ">>> Creating bundled Java runtime with jlink..."
"$JAVA_HOME/bin/jlink" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "${BUNDLED_RUNTIME}"

# Assemble standard macOS .app directory structure
echo ">>> Packaging application bundle..."
rm -rf "${PACKAGE_DIR}/${APP_NAME}.app"
mkdir -p "${APP_BUNDLE}/Contents/MacOS"
mkdir -p "${APP_BUNDLE}/Contents/Resources/bin"
mkdir -p "${APP_BUNDLE}/Contents/Resources/lib"

# Move the stripped JRE inside the bundle
mv "${BUNDLED_RUNTIME}" "${APP_BUNDLE}/Contents/runtime"

# Copy desktop build outputs (lib and scripts)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop/lib/"* "${APP_BUNDLE}/Contents/Resources/lib/"

# Remove cross-platform native libraries
LIB_DIR="${APP_BUNDLE}/Contents/Resources/lib"
rm -f "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/libpxcclr* \
      "$LIB_DIR"/zspace*

cp "${SRC_DIR}/packaging/mac/geogebra-discovery" "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"
chmod +x "${APP_BUNDLE}/Contents/MacOS/geogebra-discovery"

cp "${SRC_DIR}/packaging/mac/Info.plist" "${APP_BUNDLE}/Contents/Info.plist"

mkdir -p /tmp/AppIcon.iconset
sips -z 16 16     "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_16x16.png
sips -z 32 32     "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_16x16@2x.png
sips -z 32 32     "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_32x32.png
sips -z 64 64     "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_32x32@2x.png
sips -z 128 128   "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_128x128.png
sips -z 256 256   "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_128x128@2x.png
sips -z 256 256   "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_256x256.png
sips -z 512 512   "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_256x256@2x.png
sips -z 512 512   "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_512x512.png
sips -z 1024 1024 "${SRC_DIR}/flatpak/gd-512.png" --out /tmp/AppIcon.iconset/icon_512x512@2x.png
iconutil -c icns /tmp/AppIcon.iconset -o "${APP_BUNDLE}/Contents/Resources/app.icns"
rm -rf /tmp/AppIcon.iconset

# Create APP
mkdir -p "${OUT_DIR}"
OUTPUT_ARCHIVE="${OUT_DIR}/geogebra-discovery-${VERSION}-macos-${ARCH}.zip"

echo ">>> Creating ZIP archive: ${OUTPUT_ARCHIVE}"
(cd "${PACKAGE_DIR}" && zip -qyr "${OUTPUT_ARCHIVE}" "${APP_NAME}.app")

# Create DMG
DMG_STAGING="/tmp/ggd-dmg-staging"
OUTPUT_DMG="${OUT_DIR}/geogebra-discovery-${VERSION}-macos-${ARCH}.dmg"

echo ">>> Creating DMG distribution: ${OUTPUT_DMG}"
rm -rf "${DMG_STAGING}" "${OUTPUT_DMG}"
mkdir -p "${DMG_STAGING}"

# Copy the .app into the staging directory
cp -RP "${APP_BUNDLE}" "${DMG_STAGING}/"

# Create a symlink to /Applications for standard drag-and-drop install
ln -s /Applications "${DMG_STAGING}/Applications"

# Create compressed read-only DMG (UDZO format)
hdiutil create \
  -volname "${APP_NAME}" \
  -srcfolder "${DMG_STAGING}" \
  -ov \
  -format UDZO \
  "${OUTPUT_DMG}"

rm -rf "${DMG_STAGING}"

echo ">>> Done."