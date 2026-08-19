#!/usr/bin/env bash
set -euo pipefail

version_file="${1:-version.properties}"
gradle_properties_file="gradle.properties"

read_property() {
  local file="$1"
  local key="$2"
  sed -n "s/^${key}=//p" "$file" | tail -n1 | tr -d '[:space:]'
}

if [[ ! -f "$version_file" ]]; then echo "版本文件不存在：$version_file" >&2; return 1 2>/dev/null || exit 1; fi
if [[ ! -f "$gradle_properties_file" ]]; then echo "Gradle 配置不存在：$gradle_properties_file" >&2; return 1 2>/dev/null || exit 1; fi

VERSION_NAME="$(read_property "$version_file" VERSION_NAME)"
VERSION_CODE="$(read_property "$version_file" VERSION_CODE)"
SCHOOL_UPDATE_URL="${SCHOOL_UPDATE_URL:-$(read_property "$gradle_properties_file" schoolUpdateUrl)}"

if ! [[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then echo "VERSION_NAME 必须使用 x.y.z 格式" >&2; return 1 2>/dev/null || exit 1; fi
if ! [[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then echo "VERSION_CODE 必须是正整数" >&2; return 1 2>/dev/null || exit 1; fi
if [[ -z "$SCHOOL_UPDATE_URL" ]]; then echo "缺少 schoolUpdateUrl" >&2; return 1 2>/dev/null || exit 1; fi

update_url="${SCHOOL_UPDATE_URL%/}"
DEVELOPMENT_RELEASE_TAG="${DEVELOPMENT_RELEASE_TAG:-${update_url##*/}}"
if ! [[ "$DEVELOPMENT_RELEASE_TAG" =~ ^[A-Za-z0-9._-]+$ ]]; then echo "Development Release tag 无效：$DEVELOPMENT_RELEASE_TAG" >&2; return 1 2>/dev/null || exit 1; fi

SCHOOL_VERSION_NAME="$VERSION_NAME"
SCHOOL_VERSION_CODE="$VERSION_CODE"
export VERSION_NAME VERSION_CODE SCHOOL_VERSION_NAME SCHOOL_VERSION_CODE SCHOOL_UPDATE_URL DEVELOPMENT_RELEASE_TAG

if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "VERSION_NAME=$VERSION_NAME" >> "$GITHUB_ENV"
  echo "VERSION_CODE=$VERSION_CODE" >> "$GITHUB_ENV"
  echo "SCHOOL_VERSION_NAME=$SCHOOL_VERSION_NAME" >> "$GITHUB_ENV"
  echo "SCHOOL_VERSION_CODE=$SCHOOL_VERSION_CODE" >> "$GITHUB_ENV"
  echo "SCHOOL_UPDATE_URL=$SCHOOL_UPDATE_URL" >> "$GITHUB_ENV"
  echo "DEVELOPMENT_RELEASE_TAG=$DEVELOPMENT_RELEASE_TAG" >> "$GITHUB_ENV"
fi

echo "App 版本：$VERSION_NAME ($VERSION_CODE)"
