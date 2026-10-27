#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
export LANG="C.UTF-8"
export LC_ALL="C.UTF-8"

# Detect machine architecture
_HOST_ARCH="$(uname -m)"

case "$_HOST_ARCH" in
  x86_64|amd64)
    ;;
  *)
    echo "Unsupported architecture: $_HOST_ARCH" >&2
    exit 1
    ;;
esac

VERSION="${APP_VERSION//_/-}"
SRC_DIR="/app-src"
OUT_DIR="/out"
GGD_SRC_SHADOW="/tmp/ggd-src"
BUILD_DIR="/tmp/ggd-windows-package"
SETUP_DIR="/tmp/setup"

echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
apt-get update
apt-get install -y --no-install-recommends \
  openjdk-11-jdk wget curl libarchive-tools imagemagick nsis nsis-pluginapi

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
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Copy application files (bin and lib)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$BUILD_DIR/"

convert "$SRC_DIR/flatpak/gd-512.png" "-define" "icon:auto-resize=16,32,48,256" "$BUILD_DIR/geogebra-discovery.ico"

# Remove unneeded native binaries/jars
LIB_DIR="$BUILD_DIR/lib"
rm -f "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/*-macosx-* \
      "$LIB_DIR"/mac_extensions* \
      "$LIB_DIR"/zspace*-i586* \
      "$LIB_DIR"/libpxcclr*-i586* \
      "$LIB_DIR"/*linux.jar

# Remove the default entry scripts
rm -f "$BUILD_DIR/bin/desktop"
rm -f "$BUILD_DIR/bin/desktop.bat"

cat << 'EOF' > "${BUILD_DIR}/bin/GeoGebra-Discovery.bat"
@echo off
setlocal
set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=.\"
set "APP_HOME=%DIRNAME%.."
set "JAVA_HOME=%APP_HOME%\jre"

set "CP=%APP_HOME%\lib\*"
set "MAIN=org.geogebra.desktop.GeoGebra3D"
set "VM_ARGS=-Dnashorn.args=--no-deprecation-warning"

"%JAVA_HOME%\bin\java.exe" -cp "%CP%" %VM_ARGS% %MAIN% %*

:: If exit code was non-zero (crash/error), pause so the user can read the console
if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with error code %ERRORLEVEL%.
    pause
)

endlocal
EOF

echo ">>> Download Windows JRE..."
mkdir -p "$BUILD_DIR/jre"
JRE_URL="https://api.adoptium.net/v3/binary/latest/11/ga/windows/x64/jre/hotspot/normal/eclipse"
curl -sSL "${JRE_URL}" | bsdtar -xvf - -C "$BUILD_DIR/jre" --strip-components 1

echo ">>>Download and install NSIS EnVar plugin..."
ENVAR_TMP="/tmp/envar"
mkdir -p "${ENVAR_TMP}"
curl -sSL "https://github.com/GsNSIS/EnVar/releases/download/v0.3.1/EnVar-Plugin.zip" | \
  bsdtar -xvf - -C "${ENVAR_TMP}"

cp -rf "${ENVAR_TMP}/Plugins/"* /usr/share/nsis/Plugins/

rm -rf "${ENVAR_TMP}"

rm -rf "${SETUP_DIR}"
mkdir -p "${SETUP_DIR}"

OUTPUT_FILE_PATH="${OUT_DIR}/GeoGebra-Discovery-Setup-${VERSION}-x86_64.exe"

sed -e "s|@PKGVER@|${VERSION}|g" \
    -e "s|@BUILD_DIR@|${BUILD_DIR}|g" \
    -e "s|@OUTPUT_FILE@|${OUTPUT_FILE_PATH}|g" \
    "${SRC_DIR}/packaging/installer.nsi.in" > "${SETUP_DIR}/installer.nsi"

makensis "${SETUP_DIR}/installer.nsi"

if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${OUTPUT_FILE_PATH}"
fi