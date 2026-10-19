#!/usr/bin/env bash
set -euo pipefail

# Detect architecture
_HOST_ARCH="$(uname -m)"

case "$_HOST_ARCH" in
  x86_64)
    ;;
  *)
    echo "Error: Only x86_64 is supported. Detected: $_HOST_ARCH" >&2
    exit 1
    ;;
esac
echo ">>> Targeting architecture: x86_64"

# Variable setup
VERSION="${APP_VERSION//-/.}"
SRC_DIR="/app-src"
OUT_DIR="/out"
GGD_SRC_SHADOW="/tmp/ggd-src"
BUILD_DIR="/tmp/ggd-pacman-build"
PACKAGE_DIR="/tmp/ggd-pacman-package"


echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
pacman -Syu --noconfirm --needed \
  jdk11-openjdk sudo fakeroot
export JAVA_HOME="/usr/lib/jvm/java-11-openjdk"
export PATH="$JAVA_HOME/bin:$PATH"

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


echo ">>> Prepare packaging..."
rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}"

mkdir -p "$BUILD_DIR/opt/geogebra-discovery"
mkdir -p "$BUILD_DIR/usr/bin"
mkdir -p "$BUILD_DIR/usr/share/applications"
mkdir -p "$BUILD_DIR/usr/share/pixmaps"

# Copy application files (bin and lib)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$BUILD_DIR/opt/geogebra-discovery/"

cp "$SRC_DIR/flatpak/gd-512.png" "$BUILD_DIR/usr/share/pixmaps/geogebra-discovery.png" 2>/dev/null || true

# Remove the Windows batch script from the Linux package
rm -f "$BUILD_DIR/opt/geogebra-discovery/bin/desktop.bat"

# remove unneeded jars
LIB_DIR="$BUILD_DIR/opt/geogebra-discovery/lib"
rm -f "$LIB_DIR"/*-macosx-* \
      "$LIB_DIR"/*-windows-* \
      "$LIB_DIR"/mac_extensions* \
      "$LIB_DIR"/zspace* \
      "$LIB_DIR"/libpxcclr*

for arch in linux-amd64 linux-aarch64 linux-arm linux-i586; do
  if [ "$arch" != "linux-amd64" ]; then
    rm -f "$LIB_DIR"/*-"$arch"*
  fi
done

# Ensure execution permissions for the main runner script
chmod +x "$BUILD_DIR/opt/geogebra-discovery/bin/desktop"

ln -s "/opt/geogebra-discovery/bin/desktop" "$BUILD_DIR/usr/bin/geogebra-discovery"

cat << 'EOF' > "$BUILD_DIR/usr/share/applications/geogebra-discovery.desktop"
[Desktop Entry]
Version=1.0
Name=GeoGebra Discovery
Comment=Dynamic mathematics software
Exec=/usr/bin/geogebra-discovery %F
Icon=geogebra-discovery
Terminal=false
Type=Application
Categories=Education;Math;
EOF


echo ">>> Packaging..."

mkdir -p "${PACKAGE_DIR}"
cat << 'EOF' > "${PACKAGE_DIR}/PKGBUILD.in"
# Maintainer:
pkgname=geogebra-discovery
pkgver=@PKGVER@
pkgrel=1
pkgdesc="GeoGebra desktop application for interactive geometry, algebra, statistics and calculus."
arch=('x86_64')
url="https://github.com/kovzol/geogebra"
license=('GPL3, EUPL 1.2, Creative Commons BY-NC-SA 4.0')
depends=('java-runtime>=11' 'java-runtime<21')
options=('!debug')

package() {
  cp -a @BUILD_DIR@/* "${pkgdir}"
}
EOF

sed -e "s|@PKGVER@|${VERSION}|g" \
    -e "s|@BUILD_DIR@|${BUILD_DIR}|g" \
    "${PACKAGE_DIR}/PKGBUILD.in" > "${PACKAGE_DIR}/PKGBUILD"

# makepkg cannot run as root; create a non-root builder user
if ! id -u builder >/dev/null 2>&1; then
  useradd -m -s /bin/bash builder
  echo "builder ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
fi

chown -R builder:builder "${PACKAGE_DIR}"
cd "${PACKAGE_DIR}"
sudo -u builder makepkg -f --noconfirm


if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${PACKAGE_DIR}"/*.pkg.tar.zst || true
fi

cp -p "${PACKAGE_DIR}"/*.pkg.tar.zst "${OUT_DIR}/"
