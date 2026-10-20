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
apt-get install -y --no-install-recommends\
  openjdk-11-jdk wget curl libarchive-tools imagemagick nsis


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

# Remove the Linux executable
rm -f "$BUILD_DIR/bin/desktop"

# inject jdk path setup to desktop.bat
mv "${BUILD_DIR}/bin/desktop.bat" "${BUILD_DIR}/bin/desktop_old.bat"
cat << 'EOF' > "${BUILD_DIR}/bin/desktop.bat"
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set "JAVA_HOME=%DIRNAME%..\jre"
set "JAVACMD=%JAVA_HOME%\bin\java.exe"
EOF
cat "${BUILD_DIR}/bin/desktop_old.bat" >> "${BUILD_DIR}/bin/desktop.bat"
rm "${BUILD_DIR}/bin/desktop_old.bat"

# remove unneeded jars
LIB_DIR="$BUILD_DIR/lib"
rm -f "$LIB_DIR"/*-linux-* \
      "$LIB_DIR"/*-macosx-* \
      "$LIB_DIR"/mac_extensions* \
      "$LIB_DIR"/zspace*-i586* \
      "$LIB_DIR"/libpxcclr*-i586* \
      "$LIB_DIR"/*linux.jar

# Download windows jre
mkdir -p "$BUILD_DIR/jre"
JRE_URL="https://api.adoptium.net/v3/binary/latest/11/ga/windows/x64/jre/hotspot/normal/eclipse"
curl -sSL "${JRE_URL}" | bsdtar -xvf - -C "$BUILD_DIR/jre" --strip-components 1

rm -rf "${SETUP_DIR}"
mkdir -p "${SETUP_DIR}"

cat << 'EOF' > "${SETUP_DIR}/installer.nsi.in"
!include "MUI2.nsh"

!define APPNAME "GeoGebra Discovery"
!define APPVER "@PKGVER@"
!define COMPANY "GeoGebra"
!define OUTFILE "@OUTPUT_FILE@"
!define BUILD_DIR "@BUILD_DIR@"
!define ICON_FILE "${BUILD_DIR}/geogebra-discovery.ico"

Name "${APPNAME}"
OutFile "${OUTFILE}"
InstallDir "$PROGRAMFILES64\GeoGebra Discovery"
InstallDirRegKey HKLM "Software\${APPNAME}" "InstallDir"
RequestExecutionLevel admin

!define MUI_ICON "${ICON_FILE}"
!define MUI_UNICON "${ICON_FILE}"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES

!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_FUNCTION "LaunchApp"
!define MUI_FINISHPAGE_RUN_TEXT "Start GeoGebra Discovery now"
!define MUI_FINISHPAGE_RUN_CHECKED

!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Function .onInit
  SetRegView 64
  SetShellVarContext all
FunctionEnd

Function un.onInit
  SetRegView 64
  SetShellVarContext all
FunctionEnd

Function LaunchApp
  SetOutPath "$INSTDIR"
  Exec '"$INSTDIR\jre\bin\javaw.exe" -cp "$INSTDIR\lib\*" -Dnashorn.args=--no-deprecation-warning org.geogebra.desktop.GeoGebra3D'
FunctionEnd

Section "MainSection" SEC01
  SetOutPath "$INSTDIR"
  File /r "${BUILD_DIR}/*"

  WriteUninstaller "$INSTDIR\uninstall.exe"

  CreateDirectory "$SMPROGRAMS\${APPNAME}"

  ; 1. Standard Shortcut (Windowless via javaw.exe)
  CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" \
    "$INSTDIR\jre\bin\javaw.exe" \
    '-cp "$INSTDIR\lib\*" -Dnashorn.args=--no-deprecation-warning org.geogebra.desktop.GeoGebra3D' \
    "$INSTDIR\geogebra-discovery.ico" 0 \
    SW_SHOWNORMAL "" "${APPNAME}"

  CreateShortcut "$DESKTOP\${APPNAME}.lnk" \
    "$INSTDIR\jre\bin\javaw.exe" \
    '-cp "$INSTDIR\lib\*" -Dnashorn.args=--no-deprecation-warning org.geogebra.desktop.GeoGebra3D' \
    "$INSTDIR\geogebra-discovery.ico" 0 \
    SW_SHOWNORMAL "" "${APPNAME}"

  ; 2. Debug Shortcut (Launches via java.exe inside a visible console)
  CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME} (Debug Mode).lnk" \
    "$INSTDIR\jre\bin\java.exe" \
    '-cp "$INSTDIR\lib\*" -Dnashorn.args=--no-deprecation-warning org.geogebra.desktop.GeoGebra3D' \
    "$INSTDIR\geogebra-discovery.ico" 0 \
    SW_SHOWNORMAL "" "${APPNAME} with Console Output"

  ; 3. Uninstaller Shortcut
  CreateShortcut "$SMPROGRAMS\${APPNAME}\Uninstall ${APPNAME}.lnk" \
    "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0

  ; --- Windows Add/Remove Programs Registration ---
  !define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"

  WriteRegStr HKLM "${UNINST_KEY}" "DisplayName" "${APPNAME}"
  WriteRegStr HKLM "${UNINST_KEY}" "DisplayVersion" "${APPVER}"
  WriteRegStr HKLM "${UNINST_KEY}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\geogebra-discovery.ico"
  WriteRegStr HKLM "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "${UNINST_KEY}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr HKLM "${UNINST_KEY}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegDWORD HKLM "${UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKLM "${UNINST_KEY}" "NoRepair" 1
  WriteRegDWORD HKLM "${UNINST_KEY}" "EstimatedSize" 166000
SectionEnd

Section "Uninstall"
  Delete "$DESKTOP\${APPNAME}.lnk"
  Delete "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk"
  Delete "$SMPROGRAMS\${APPNAME}\${APPNAME} (Debug Mode).lnk"
  Delete "$SMPROGRAMS\${APPNAME}\Uninstall ${APPNAME}.lnk"
  RMDir "$SMPROGRAMS\${APPNAME}"

  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\jre"
  Delete "$INSTDIR\geogebra-discovery.ico"
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
  DeleteRegKey HKLM "Software\${APPNAME}"
SectionEnd
EOF

OUTPUT_FILE_PATH="${OUT_DIR}/GeoGebra-Discovery-Setup-${VERSION}-x86_64.exe"

sed -e "s|@PKGVER@|${VERSION}|g" \
    -e "s|@BUILD_DIR@|${BUILD_DIR}|g" \
    -e "s|@OUTPUT_FILE@|${OUTPUT_FILE_PATH}|g" \
    "${SETUP_DIR}/installer.nsi.in" > "${SETUP_DIR}/installer.nsi"

makensis "${SETUP_DIR}/installer.nsi"

if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${OUTPUT_FILE_PATH}"
fi
