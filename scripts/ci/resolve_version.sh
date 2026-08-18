#!/usr/bin/env bash
set -euo pipefail

version_file="${1:-version.properties}"

read_property() {
  local key="$1"
  sed -n "s/^${key}=//p" "$version_file" | tail -n1 | tr -d '[:space:]'
}

if [[ ! -f "$version_file" ]]; then
  echo "版本文件不存在：$version_file" >&2
  exit 1
fi

name="$(read_property VERSION_NAME)"
code="$(read_property VERSION_CODE)"

if ! [[ "$name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "VERSION_NAME 必须使用 x.y.z 格式" >&2
  exit 1
fi

if ! [[ "$code" =~ ^[1-9][0-9]*$ ]]; then
  echo "VERSION_CODE 必须是正整数" >&2
  exit 1
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "version_name=$name" >> "$GITHUB_OUTPUT"
  echo "version_code=$code" >> "$GITHUB_OUTPUT"
fi

echo "App 版本：$name ($code)"
