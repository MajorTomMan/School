#!/usr/bin/env bash
set -euo pipefail

: "${VERSION_NAME:?缺少 VERSION_NAME}"
: "${VERSION_CODE:?缺少 VERSION_CODE}"

tag="${DEVELOPMENT_RELEASE_TAG:-dev-latest}"
version_changed=false
code_changed=false
should_publish=false

git fetch --force origin "refs/tags/$tag:refs/tags/$tag" >/dev/null 2>&1 || true

if git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
  previous_name="$(git show "refs/tags/$tag:version.properties" | sed -n 's/^VERSION_NAME=//p' | tail -n1 | tr -d '[:space:]')"
  previous_code="$(git show "refs/tags/$tag:version.properties" | sed -n 's/^VERSION_CODE=//p' | tail -n1 | tr -d '[:space:]')"
  if [[ "$VERSION_NAME" != "$previous_name" && "$VERSION_CODE" -gt "$previous_code" ]]; then version_changed=true; fi
  if ! git diff --quiet "refs/tags/$tag..HEAD" -- app visualization build.gradle.kts settings.gradle.kts gradle.properties gradle signing scripts/ci; then code_changed=true; fi
else
  version_changed=true
  code_changed=true
fi

if [[ "$version_changed" == "true" && "$code_changed" == "true" ]]; then should_publish=true; fi
export SHOULD_PUBLISH="$should_publish"
if [[ -n "${GITHUB_ENV:-}" ]]; then echo "SHOULD_PUBLISH=$should_publish" >> "$GITHUB_ENV"; fi

echo "发布检测：version_changed=$version_changed, code_changed=$code_changed, should_publish=$should_publish"
