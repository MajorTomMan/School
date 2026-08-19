#!/usr/bin/env bash
set -euo pipefail

tests=(
  "com.majortomman.school.data.material.EducationStageModelsTest"
  "com.majortomman.school.update.UpdateManifestCodecTest"
  "com.majortomman.school.data.math.MathExpressionEngineTest"
  "com.majortomman.school.learning.science.MathFoundationTest"
  "com.majortomman.school.learning.science.MathFormulaVerifierTest"
  "com.majortomman.school.learning.verification.math.MathVerificationEngineTest"
)

args=()
for test_class in "${tests[@]}"; do
  args+=(--tests "$test_class")
done

gradle :app:testDebugUnitTest :app:assembleDebug "${args[@]}" --stacktrace
