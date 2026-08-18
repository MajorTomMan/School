#!/usr/bin/env bash
set -euo pipefail

temp_root="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"
keystore="$(mktemp "${temp_root%/}/school-development.XXXXXX.jks")"
trap 'rm -f "$keystore"' EXIT

base64 --decode signing/school-development.jks.b64 > "$keystore"
expected="$(tr -d '[:space:]:' < signing/school-development.cert.sha256 | tr '[:upper:]' '[:lower:]')"
actual="$(keytool -list -v -keystore "$keystore" -storepass android -alias schooldev | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"

test -n "$actual"
test "$actual" = "$expected"

echo "Development signing key 校验通过"
