#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

source_roots=(
  "app/src/main/java"
  "app/src/test/java"
  "visualization/src/main/java"
  "visualization/src/test/java"
)

forbidden_symbols=(
  "CourseExplanation"
  "CourseFormula"
  "CourseVisualizationStep"
  "CoursePractice"
  "CourseCheckpoint"
  "MathQuestionBankRepository"
  "SchoolDatabase"
  "LearningDao"
  "ReviewScheduler"
  "MasteryStatus"
  "DailyPlan"
  "CourseStorageUpdateCheck"
  "VerificationSubject"
  "VerificationMode"
  "SubjectEngine"
  "CapabilityRegistry"
  "SchoolCapabilityCatalog"
  "CapabilityDescriptor"
  "CapabilityKind"
  "SubjectModule"
  "SubjectId"
  "VerificationVisualizationRequest"
  "VerificationVisualizationValue"
  "LegacyCourseParser"
)

forbidden_paths=(
  "app/src/main/java/com/majortomman/school/data/PreferencesRepository.kt"
  "app/src/main/java/com/majortomman/school/data/local/LearningDao.kt"
  "app/src/main/java/com/majortomman/school/data/local/SchoolDatabase.kt"
  "app/src/main/java/com/majortomman/school/data/local/PracticeAttemptEntity.kt"
  "app/src/main/java/com/majortomman/school/data/local/ReviewScheduleEntity.kt"
  "app/src/main/java/com/majortomman/school/data/local/MathPracticeAttemptEntity.kt"
  "app/src/main/java/com/majortomman/school/data/local/MathKnowledgeMasteryEntity.kt"
  "app/src/main/java/com/majortomman/school/data/local/MathMistakeEntity.kt"
  "app/src/main/java/com/majortomman/school/data/review/ReviewScheduler.kt"
  "app/src/main/java/com/majortomman/school/data/math/MathExpressionEngine.kt"
  "app/src/main/java/com/majortomman/school/data/math/MathQuestionBankRepository.kt"
  "app/src/main/java/com/majortomman/school/data/math/MathQuestionModels.kt"
  "app/src/main/java/com/majortomman/school/data/math/MathQuestionTemplates.kt"
  "app/src/main/java/com/majortomman/school/ui/MathQuestionBankScreen.kt"
  "app/src/main/java/com/majortomman/school/ui/MathQuestionBankActions.kt"
  "app/src/main/java/com/majortomman/school/ui/LessonPresentation.kt"
  "app/src/main/java/com/majortomman/school/ui/AssessmentLearningContentRenderer.kt"
  "app/src/main/java/com/majortomman/school/ui/CloudCourseBlockRenderer.kt"
  "app/src/main/java/com/majortomman/school/learning/verification/VerificationHubModels.kt"
  "app/src/main/java/com/majortomman/school/learning/verification/DiagnosticModels.kt"
  "app/src/main/java/com/majortomman/school/learning/cloud/CourseCapabilityCompatibilityValidator.kt"
  "app/src/main/java/com/majortomman/school/learning/capability/LearningCapability.kt"
  "app/src/main/java/com/majortomman/school/ui/VerificationHubScreen.kt"
)

failed=0

for path in "${forbidden_paths[@]}"; do
  if [[ -e "$path" ]]; then
    echo "Framework v1 guard: legacy path reintroduced: $path" >&2
    failed=1
  fi
done

for symbol in "${forbidden_symbols[@]}"; do
  matches="$(grep -RInw --include='*.kt' --include='*.java' -- "$symbol" "${source_roots[@]}" 2>/dev/null || true)"
  if [[ -n "$matches" ]]; then
    echo "Framework v1 guard: legacy symbol reintroduced: $symbol" >&2
    echo "$matches" >&2
    failed=1
  fi
done

ui_verifier_matches="$(grep -RInw --include='*.kt' --include='*.java' -- "MathFormulaVerifier" "app/src/main/java/com/majortomman/school/ui" 2>/dev/null || true)"
if [[ -n "$ui_verifier_matches" ]]; then
  echo "Framework v1 guard: UI must not invoke MathFormulaVerifier directly" >&2
  echo "$ui_verifier_matches" >&2
  failed=1
fi

ui_activity_runtime_matches="$(grep -RInw --include='*.kt' --include='*.java' -- "ActivityRuntime" "app/src/main/java/com/majortomman/school/ui" 2>/dev/null || true)"
if [[ -n "$ui_activity_runtime_matches" ]]; then
  echo "Framework v1 guard: UI must not own ActivityRuntime" >&2
  echo "$ui_activity_runtime_matches" >&2
  failed=1
fi

if (( failed != 0 )); then
  exit 1
fi

echo "Framework v1 legacy boundary guard passed."
