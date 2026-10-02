#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CI_DIR="$ROOT/scripts/ci"
cd "$ROOT"

bash "$CI_DIR/check_framework_v1_boundaries.sh"

if [[ -x "./gradlew" ]]; then
  gradle_cmd=(./gradlew)
else
  gradle_cmd=(gradle)
fi

"${gradle_cmd[@]}" :app:testDebugUnitTest :visualization:testDebugUnitTest --stacktrace
"${gradle_cmd[@]}" :app:assembleDebug --stacktrace
