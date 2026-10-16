#!/usr/bin/env bash
set -euo pipefail

# Detect architecture
_HOST_ARCH="$(uname -m)"

case "$_HOST_ARCH" in
  x86_64)
    ARCH="x86_64"
    KEEP_NATIVE="linux-amd64"
    ;;
  aarch64)
    ARCH="aarch64"
    KEEP_NATIVE="linux-aarch64"
    ;;
  *)
    echo "Error: Only x86_64 and aarch64 are supported. Detected: $_HOST_ARCH" >&2
    exit 1
    ;;
esac
echo ">>> Targeting architecture: RPM ($ARCH), Java native string ($KEEP_NATIVE)"

# Variable setup
# VERSION expects a clean and valid version string
VERSION="${APP_VERSION}"
SRC_DIR="/app-src"
OUT_DIR="/out"
GGD_SRC_SHADOW="/tmp/ggd-src"
PACKAGE_DIR="/tmp/ggd-rpm-package"


echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
dnf install -y git java-11-openjdk-devel rpm-build tree
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


echo ">>> Packaging Geogebra-discovery..."
rm -rf "$PACKAGE_DIR"

mkdir -p "$PACKAGE_DIR/opt/geogebra-discovery"
mkdir -p "$PACKAGE_DIR/usr/bin"
mkdir -p "$PACKAGE_DIR/usr/share/applications"
mkdir -p "$PACKAGE_DIR/usr/share/pixmaps"

# Copy application files (bin and lib)
cp -r "${GGD_SRC_SHADOW}/desktop/build/install/desktop"/* "$PACKAGE_DIR/opt/geogebra-discovery/"

cp "$SRC_DIR/flatpak/gd-512.png" "$PACKAGE_DIR/usr/share/pixmaps/geogebra.png" 2>/dev/null || true

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
Comment=Dynamic mathematics software
Exec=/usr/bin/geogebra %F
Icon=geogebra
Terminal=false
Type=Application
Categories=Education;Math;
EOF


mkdir -p ~/rpmbuild/{BUILD,RPMS,SOURCES,SPECS,SRPMS}

cat << 'EOF' > ~/rpmbuild/SPECS/geogebra.spec
Name:           geogebra-discovery
Version:        %{rpm_version}
Release:        1%{?dist}
Summary:        Dynamic mathematics software for education
License:        GPLv3+
URL:            https://www.geogebra.org
AutoReqProv:    no

Requires:       java >= 1:1.8.0

%description
GeoGebra desktop application for interactive geometry,
algebra, statistics and calculus.

%install
mkdir -p %{buildroot}
cp -a %{staging_dir}/. %{buildroot}/

%files
/opt/geogebra-discovery
/usr/bin/geogebra-discovery
/usr/share/applications/geogebra-discovery.desktop
/usr/share/pixmaps/geogebra.png
EOF

rpmbuild -bb \
  --define "staging_dir ${PACKAGE_DIR}" \
  --define "rpm_version ${VERSION}" \
  ~/rpmbuild/SPECS/geogebra.spec

if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "$HOME"/rpmbuild/RPMS/*/*.rpm || true
fi

cp -p "$HOME"/rpmbuild/RPMS/*/*.rpm "${OUT_DIR}/"
