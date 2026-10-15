#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

# Detect machine architecture
_RAW_ARCH="$(uname -m)"
case "${_RAW_ARCH}" in
  x86_64)
    ARCH="x86_64"
    ;;
  aarch64|arm64)
    ARCH="aarch64"
    ;;
  *)
    echo "Unsupported architecture: ${_RAW_ARCH}" && exit 1
    ;;
esac
export ARCH
echo ">>> Detected target architecture: ${ARCH}"

# Expects a clean and valid version string
VERSION="${APP_VERSION:-dev}"
SRC_DIR="/app-src"
OUT_DIR="/out"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-deb-package"

echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
apt-get update
apt-get install -y --no-install-recommends\
  openjdk-11-jdk tree


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


echo ">>> Building Geogebra-discovery..."
cd "${GGD_SRC_SHADOW}/"
./gradlew :desktop:installDist


echo ">>> Packaging Geogebra-discovery..."
rm -rf "$PACKAGE_DIR"

# Create standard Debian directory hierarchy
mkdir -p "$PACKAGE_DIR/DEBIAN"
mkdir -p "$PACKAGE_DIR/opt/geogebra-discovery"
mkdir -p "$PACKAGE_DIR/usr/bin"
mkdir -p "$PACKAGE_DIR/usr/share/applications"
mkdir -p "$PACKAGE_DIR/usr/share/pixmaps"

# Copy application files (bin and lib)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$PACKAGE_DIR/opt/geogebra-discovery/"

# Remove the Windows batch script from the Linux package
rm -f "$PACKAGE_DIR/opt/geogebra-discovery/bin/desktop.bat"

# Ensure execution permissions for the main runner script
chmod +x "$PACKAGE_DIR/opt/geogebra-discovery/bin/desktop"

ln -s "/opt/geogebra-discovery/bin/desktop" "$PACKAGE_DIR/usr/bin/geogebra-discovery"

cat > "$PACKAGE_DIR/DEBIAN/control" <<EOF
Package: geogebra-discovery
Version: ${VERSION}
Section: math
Priority: optional
Architecture: all
Depends: default-jre | openjdk-11-jre | openjdk-8-jre
Maintainer: Your Name <you@example.com>
Description: GeoGebra Dynamic Mathematics Software
 GeoGebra desktop application for interactive geometry,
 algebra, statistics and calculus.
EOF

cat << 'EOF' > "$PACKAGE_DIR/usr/share/applications/geogebra.desktop"
[Desktop Entry]
Version=1.0
Name=GeoGebra Discovery
Comment=Dynamic mathematics software
Exec=/usr/bin/geogebra %F
Icon=geogebra
Terminal=false
Type=Application
Categories=Education;Math;
EOF

cp "$SRC_DIR/flatpak/gd-512.png" "$PACKAGE_DIR/usr/share/pixmaps/geogebra.png" 2>/dev/null || true

chown -R root:root "$PACKAGE_DIR"
chmod 755 "$PACKAGE_DIR"/DEBIAN
chmod 644 "$PACKAGE_DIR"/DEBIAN/control

OUT_DEB_PATH="${OUT_DIR}/Geogebra-Discovery-${VERSION}-${ARCH}.deb"
dpkg-deb --build --root-owner-group "$PACKAGE_DIR" "$OUT_DEB_PATH"

if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${OUT_DEB_PATH}" || true
fi
