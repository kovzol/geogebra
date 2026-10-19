#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

# Detect machine architecture
_HOST_ARCH="$(uname -m)"

case "$_HOST_ARCH" in
  x86_64|amd64)
    ARCH="x86_64"
    JRE_ARCH="x64"
    KEEP_NATIVE="linux-amd64"
    ;;
  aarch64|arm64)
    ARCH="aarch64"
    JRE_ARCH="aarch64"
    KEEP_NATIVE="linux-aarch64"
    ;;
  *)
    echo "Unsupported architecture: $_HOST_ARCH" >&2
    exit 1
    ;;
esac
export ARCH
echo ">>> Targeting architecture: $ARCH, Java native string ($KEEP_NATIVE)"

VERSION="${APP_VERSION//_/-}"
SRC_DIR="/app-src"
OUT_DIR="/out"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-deb-package"

echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
apt-get update
apt-get install -y --no-install-recommends\
  openjdk-11-jdk wget file curl


echo ">>> Copying project source files..."
rm -rf "${GGD_SRC_SHADOW}"
mkdir -p "${GGD_SRC_SHADOW}"

cp -r "${SRC_DIR}/desktop" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/gradle" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/gradlew" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/build.gradle" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/init.gradle" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/presettings.gradle" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/settings.gradle" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/retex" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/gradle-scripts" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/carota-web" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/common" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/common-jre" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/jogl2" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/input3D" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/giac-jni" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/tarski-jni" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/web" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/ggbjdk" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/keyboard-base" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/keyboard-scientific" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/keyboard-web" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/test" "${GGD_SRC_SHADOW}/"
cp -r "${SRC_DIR}/xr-base" "${GGD_SRC_SHADOW}/"


echo ">>> Building..."
cd "${GGD_SRC_SHADOW}/"
./gradlew :desktop:installDist


echo ">>> Prepare packaging..."
rm -rf "$PACKAGE_DIR"

# Create standard Debian directory hierarchy
mkdir -p "$PACKAGE_DIR/opt/geogebra-discovery"
mkdir -p "$PACKAGE_DIR/usr/bin"
mkdir -p "$PACKAGE_DIR/usr/share/applications"
mkdir -p "$PACKAGE_DIR/usr/share/pixmaps"

# Copy application files (bin and lib)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$PACKAGE_DIR/opt/geogebra-discovery/"

cp "$SRC_DIR/flatpak/gd-512.png" "$PACKAGE_DIR/usr/share/pixmaps/geogebra-discovery.png" 2>/dev/null || true

# Remove the Windows batch script from the Linux package
rm -f "$PACKAGE_DIR/opt/geogebra-discovery/bin/desktop.bat"

# remove unneeded jars
LIB_DIR="$PACKAGE_DIR/opt/geogebra-discovery/lib"
rm -f "$LIB_DIR"/*-macosx-* \
      "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/mac_extensions* \
      "$LIB_DIR"/zspace* \
      "$LIB_DIR"/libpxcclr*

for arch in linux-amd64 linux-aarch64 linux-arm linux-i586; do
  if [ "$arch" != "$KEEP_NATIVE" ]; then
    rm -f "$LIB_DIR"/*-"$arch"*
  fi
done


# Ensure execution permissions for the main runner script
chmod +x "$PACKAGE_DIR/opt/geogebra-discovery/bin/desktop"

ln -s "/opt/geogebra-discovery/bin/desktop" "$PACKAGE_DIR/usr/bin/geogebra-discovery"

cat << 'EOF' > "$PACKAGE_DIR/usr/share/applications/geogebra-discovery.desktop"
[Desktop Entry]
Version=1.0
Name=GeoGebra Discovery
Comment=An experimental version of GeoGebra
Exec=/usr/bin/geogebra-discovery %F
Icon=geogebra-discovery
Terminal=false
Type=Application
Categories=Education;Math;
EOF

# download jre
JRE_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jre_${JRE_ARCH}_linux_hotspot_17.0.20.1_1.tar.gz"

mkdir -p "${PACKAGE_DIR}/opt/geogebra-discovery/jre"
curl -sSL "${JRE_URL}" | tar -xz -C "${PACKAGE_DIR}/opt/geogebra-discovery/jre" --strip-components=1

# create appimage related files
cat << 'EOF' > "$PACKAGE_DIR/AppRun"
#!/bin/sh
HERE=$(dirname "$(readlink -f "$0")")

export JAVA_HOME="$HERE/opt/geogebra-discovery/jre"
export PATH="$JAVA_HOME/bin:$PATH"

exec "$HERE/opt/geogebra-discovery/bin/desktop" "$@"
EOF
chmod +x "$PACKAGE_DIR/AppRun"

ln -s "$PACKAGE_DIR/usr/share/applications/geogebra-discovery.desktop" "$PACKAGE_DIR/geogebra-discovery.desktop"
ln -s "$PACKAGE_DIR/usr/share/pixmaps/geogebra-discovery.png" "$PACKAGE_DIR/geogebra-discovery.png"

echo ">>> Packaging..."
WORKDIR_DEPLOY="/tmp/linuxdeploy"
mkdir -p "${WORKDIR_DEPLOY}" && cd "${WORKDIR_DEPLOY}"

wget https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage
chmod +x "appimagetool-${ARCH}.AppImage"

export APPIMAGE_EXTRACT_AND_RUN=1

OUTPUT_APPIMAGE_NAME="geogebra-discovery-${VERSION}-${ARCH}.AppImage"
"./appimagetool-${ARCH}.AppImage" "${PACKAGE_DIR}" "${OUT_DIR}/${OUTPUT_APPIMAGE_NAME}"

if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${OUT_DIR}/${OUTPUT_APPIMAGE_NAME}" || true
fi