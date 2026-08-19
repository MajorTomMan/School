#!/usr/bin/env bash
set -euo pipefail

version_file="${1:-version.properties}"

read_property() {
  local key="$1"
  sed -n "s/^${key}=//p" "$version_file" | tail -n1 | tr -d '[:space:]'
}

if [[ ! -f "$version_file" ]]; then
  echo "版本文件不存在：$version_file" >&2
  return 1 2>/dev/null || exit 1
fi

VERSION_NAME="$(read_property VERSION_NAME)"
VERSION_CODE="$(read_property VERSION_CODE)"
DEVELOPMENT_RELEASE_TAG="${DEVELOPMENT_RELEASE_TAG:-dev-latest}"

if ! [[ "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "VERSION_NAME 必须使用 x.y.z 格式" >&2
  return 1 2>/dev/null || exit 1
fi
if ! [[ "$VERSION_CODE" =~ ^[1-9][0-9]*$ ]]; then
  echo "VERSION_CODE 必须是正整数" >&2
  return 1 2>/dev/null || exit 1
fi
if ! [[ "$DEVELOPMENT_RELEASE_TAG" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Development Release tag 无效：$DEVELOPMENT_RELEASE_TAG" >&2
  return 1 2>/dev/null || exit 1
fi

SCHOOL_VERSION_NAME="$VERSION_NAME"
SCHOOL_VERSION_CODE="$VERSION_CODE"
export VERSION_NAME VERSION_CODE SCHOOL_VERSION_NAME SCHOOL_VERSION_CODE DEVELOPMENT_RELEASE_TAG

if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "VERSION_NAME=$VERSION_NAME" >> "$GITHUB_ENV"
  echo "VERSION_CODE=$VERSION_CODE" >> "$GITHUB_ENV"
  echo "SCHOOL_VERSION_NAME=$SCHOOL_VERSION_NAME" >> "$GITHUB_ENV"
  echo "SCHOOL_VERSION_CODE=$SCHOOL_VERSION_CODE" >> "$GITHUB_ENV"
  echo "DEVELOPMENT_RELEASE_TAG=$DEVELOPMENT_RELEASE_TAG" >> "$GITHUB_ENV"
fi

echo "App 版本：$VERSION_NAME ($VERSION_CODE)"
