#!/usr/bin/env bash
set -euo pipefail

tests=(
  "com.majortomman.school.update.UpdateManifestCodecTest"
  "com.majortomman.school.data.math.MathExpressionEngineTest"
  "com.majortomman.school.learning.science.MathFoundationTest"
  "com.majortomman.school.learning.science.MathFormulaVerifierTest"
  "com.majortomman.school.learning.verification.math.MathVerificationEngineTest"
)

args=()
for test_class in "${tests[@]}"; do args+=(--tests "$test_class"); done
if [[ -x "./gradlew" ]]; then gradle_cmd=(./gradlew); else gradle_cmd=(gradle); fi

"${gradle_cmd[@]}" :app:testDebugUnitTest "${args[@]}" --stacktrace
"${gradle_cmd[@]}" :app:assembleDebug --stacktrace
