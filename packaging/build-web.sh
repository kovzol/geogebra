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

echo ">>> Target version: ${VERSION}"

echo ">>> Installing build tools and dependencies"
apt-get update
apt-get install -y --no-install-recommends\
  openjdk-11-jdk zip


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
GGD_NAME=GeoGebra-Discovery-${VERSION}

cd "${GGD_SRC_SHADOW}/"
./gradlew :web:deployIntoWar -Pgdraft

WAR_DIR="${GGD_SRC_SHADOW}/web/war"
test -r $WAR_DIR/web3d/web3d.nocache.js

STAGE_DIR="/tmp/${GGD_NAME}"
mkdir -p "${STAGE_DIR}"
cp $WAR_DIR/classic.html "${STAGE_DIR}/index.html"
cp -ur $WAR_DIR/web3d "${STAGE_DIR}"

ZIP_PATH="${OUT_DIR}/${GGD_NAME}.zip"

cd "$(dirname "${STAGE_DIR}")"
zip -9r "${ZIP_PATH}" "$(basename "${STAGE_DIR}")"


if [ -n "${HOST_UID:-}" ] && [ -n "${HOST_GID:-}" ]; then
  chown -hR "${HOST_UID}:${HOST_GID}" "${ZIP_PATH}"
fi