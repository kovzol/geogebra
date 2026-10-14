#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

SRC_DIR="/app-src"
OUT_DIR="/out"
BUILD_DIR="/tmp/gclc-deb-build"
GCLC_SRC_SHADOW="/tmp/gclc-src"

# Standardize version string
# - Strip leading 'v'
# - Replace underscores and git-describe hyphens with dots/pluses for Debian packaging policy
RAW_VER="${APP_VERSION:-dev}"
CLEAN_VER="${RAW_VER#v}"
CLEAN_VER="${CLEAN_VER//_/-}"
# Debian version numbers can only have one hyphen before the debian revision.
# If git describe gave e.g. 2026.08-12-g92498c0, turn it into 2026.08+12.g92498c0
CLEAN_VER="$(echo "${CLEAN_VER}" | sed -E 's/-([0-9]+)-g([0-9a-f]+)/+\1.\2/')"

echo ">>> Target version: ${CLEAN_VER}"

echo "Hello world from build-deb.sh"