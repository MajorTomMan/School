#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?缺少 GH_TOKEN}"
: "${VERSION_NAME:?缺少 VERSION_NAME}"

repo="${GITHUB_REPOSITORY:-MajorTomMan/School}"
sha="${GITHUB_SHA:-$(git rev-parse HEAD)}"
tag="${DEVELOPMENT_RELEASE_TAG:-dev-latest}"
title="School ${VERSION_NAME}"
notes_file=".release-notes/current.md"
assets=(dist/school-debug.apk dist/school-debug.apk.sha256 dist/update-manifest.json dist/update-manifest.sig)

for asset in "${assets[@]}"; do
  if [[ ! -f "$asset" ]]; then
    echo "发布文件不存在：$asset" >&2
    exit 1
  fi
done

if [[ ! -f "$notes_file" ]]; then
  echo "发布说明不存在：$notes_file" >&2
  exit 1
fi

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git tag -f "$tag" "$sha"
git push origin "refs/tags/$tag" --force

if gh release view "$tag" --repo "$repo" >/dev/null 2>&1; then
  gh release edit "$tag" --repo "$repo" --title "$title" --notes-file "$notes_file" --prerelease
  gh release upload "$tag" "${assets[@]}" --repo "$repo" --clobber
else
  gh release create "$tag" "${assets[@]}" --repo "$repo" --target "$sha" --title "$title" --notes-file "$notes_file" --prerelease
fi

echo "Development Release 已发布：$tag -> $sha"
