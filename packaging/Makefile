MAKEFILE_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))
REPO_ROOT    := $(abspath $(MAKEFILE_DIR)/..)

# Fetch raw describe (e.g., "v1.2.0-4-g9e2f1a-dirty" or "9e2f1a")
_GIT_RAW := $(shell git -C $(REPO_ROOT) describe --tags --always --dirty 2>/dev/null || echo "0.0.0")
# Strip leading 'v'
_GIT_STRIPPED := $(patsubst v%,%,$(_GIT_RAW))
# Ensure leading digit if no tag exists (e.g., '9e2f1a' -> '0.0.0.git.9e2f1a')
_SAFE_BASE := $(shell echo "$(_GIT_STRIPPED)" | sed -E 's/^([^0-9])/0.0.0.git.\1/')
# Replace hyphens with dots:
#    - Turn "-4-g9e2f1a" into ".post4.g9e2f1a"
#    - Turn "-dirty" into ".dirty"
#    - Replace any remaining forbidden characters
VERSION ?= $(shell echo "$(_SAFE_BASE)" | sed -E 's/-([0-9]+)-g([a-f0-9]+)/.post\1.g\2/' | sed 's/-dirty/.dirty/' | tr '-' '.')

ifeq ($(shell echo "$(VERSION)" | grep -Eq '^[0-9]' && echo yes),)
    $(error Invalid VERSION '$(VERSION)': must start with a digit)
endif

CACHE_DIR    ?= $(REPO_ROOT)/.build-cache
OUT_DIR      ?= $(REPO_ROOT)/dist

DOCKER_UBUNTU ?= ubuntu:22.04
DOCKER_FEDORA ?= fedora:latest
DOCKER_ARCH ?= archlinux:latest

HOST_UID := $(shell id -u)
HOST_GID := $(shell id -g)

# Architecture handling (defaults to current host machine)
ARCH ?= $(shell uname -m)

ifeq ($(ARCH),aarch64)
  DOCKER_PLATFORM := linux/arm64
else ifeq ($(ARCH),arm64)
  DOCKER_PLATFORM := linux/arm64
else
  DOCKER_PLATFORM := linux/amd64
endif

.PHONY: help man appimage deb rpm pacman windows clean-cache clean-dist

help:
	@echo "Available packaging targets:"
	@echo "  make deb               - Build Debian (.deb) package"

deb:
	@mkdir -p "$(OUT_DIR)"
	docker run --rm \
	  --platform $(DOCKER_PLATFORM) \
	  -v "$(REPO_ROOT):/app-src:ro" \
	  -v "$(MAKEFILE_DIR)/build-deb.sh:/build-deb.sh:ro" \
	  -v "$(OUT_DIR):/out" \
	  -e APP_VERSION="$(VERSION)" \
	  -e HOST_UID="$(HOST_UID)" \
	  -e HOST_GID="$(HOST_GID)" \
	  $(DOCKER_UBUNTU) /build-deb.sh

clean-dist:
	rm -rf "$(OUT_DIR)"

clean-cache:
	rm -rf "$(CACHE_DIR)"