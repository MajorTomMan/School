#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CI_DIR="$ROOT/scripts/ci"
VERSION_FILE="$ROOT/version.properties"
cd "$ROOT"

ensure_version() {
  if [[ -z "${VERSION_NAME:-}" || -z "${VERSION_CODE:-}" ]]; then
    source "$CI_DIR/resolve_version.sh" "$VERSION_FILE"
  fi
}

case "${1:-}" in
  prepare)
    source "$CI_DIR/resolve_version.sh" "$VERSION_FILE"
    bash "$CI_DIR/detect_development_release.sh"
    sdkmanager "platforms;android-36" "build-tools;36.0.0"
    bash "$CI_DIR/verify_development_signing_key.sh"
    ;;
  build)
    bash "$CI_DIR/test_app_core.sh"
    ;;
  package)
    ensure_version
    python3 "$CI_DIR/prepare_development_update.py"
    ;;
  publish)
    ensure_version
    bash "$CI_DIR/publish_development_release.sh"
    ;;
  *)
    echo "用法：$0 {prepare|build|package|publish}" >&2
    exit 2
    ;;
esac
