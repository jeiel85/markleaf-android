#!/usr/bin/env bash
#
# Publish a GitLab Release for the current tag from GitHub Actions (#252).
#
# GitLab's own tag pipeline used to do this (`publish_gitlab_release` in
# .gitlab-ci.yml). It stopped: the free shared-runner minutes are a monthly
# quota, the quota ran out, and every pipeline since has failed with
# `ci_quota_exceeded` before reaching the release stage. Eight releases shipped
# that way -- v2.28.0 through v2.31.0 -- while GitLab's newest Release stayed at
# v2.27.2, and D064 went on describing GitLab as the off-machine permanent copy
# of the AAB and mapping the whole time.
#
# The Packages and Releases APIs do not consume CI minutes. Only the *pipeline*
# did. So this runs on the GitHub runner, which is free for public
# repositories, and uploads the same four files to the same Generic Package
# Registry path glab used -- existing v2.27.2 links keep working and new ones
# match them.
#
# The tag must already exist on GitLab. Release order is GitLab first, GitHub
# second (docs/mirror-runbook.md), so by the time the GitHub tag job runs it
# does; this fails loudly rather than creating a tag, because creating one here
# would invert that order.
#
# Requires GITLAB_TOKEN with the `api` scope. The mirror-push token only needs
# `write_repository`, which is not enough to write packages or releases -- a
# token with the narrower scope fails at the first upload with 401/403 and the
# message says so.

set -euo pipefail

PROJECT="jeiel85%2Fmarkleaf-android"
API="https://gitlab.com/api/v4/projects/${PROJECT}"
PACKAGE_NAME="markleaf-android"
DIST_DIR="${DIST_DIR:-dist}"

TAG="${GITHUB_REF_NAME:?GITHUB_REF_NAME is not set}"

if [ -z "${GITLAB_TOKEN:-}" ]; then
  echo "::error::GITLAB_TOKEN secret is missing. Create a GitLab Project Access Token with the 'api' scope and register it -- see docs/mirror-runbook.md."
  exit 1
fi

auth=(--header "PRIVATE-TOKEN: ${GITLAB_TOKEN}")

# Fail on a token that cannot do the job, before uploading anything. A
# write_repository-only token reads fine and writes nothing, so the first
# meaningful check is a write-scoped read: the packages list.
probe_status="$(curl -sS -o /dev/null -w '%{http_code}' "${auth[@]}" "${API}/packages?per_page=1" || true)"
case "$probe_status" in
  200) ;;
  401|403)
    echo "::error::GITLAB_TOKEN was rejected for the Packages API (HTTP ${probe_status}). The token needs the 'api' scope; 'write_repository' alone cannot publish packages or releases."
    exit 1
    ;;
  *)
    echo "::error::Could not reach the GitLab Packages API (HTTP ${probe_status}). Not treating that as success -- the mirror is unverified."
    exit 1
    ;;
esac

# The tag has to be on GitLab already. Creating it here would invert the
# documented GitLab-first order and could double-fire the release paths.
tag_status="$(curl -sS -o /dev/null -w '%{http_code}' "${auth[@]}" "${API}/repository/tags/${TAG}" || true)"
if [ "$tag_status" != "200" ]; then
  echo "::error::Tag ${TAG} is not on GitLab (HTTP ${tag_status}). Push the tag to GitLab first, then re-run this job."
  exit 1
fi

# Already published: re-running the tag job must not fail, and must not
# silently produce a second release either.
release_status="$(curl -sS -o /dev/null -w '%{http_code}' "${auth[@]}" "${API}/releases/${TAG}" || true)"
if [ "$release_status" = "200" ]; then
  echo "GitLab already has a Release for ${TAG}; nothing to do."
  echo "GitLab Release \`${TAG}\` already existed -- left untouched." >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
  exit 0
fi

shopt -s nullglob
files=("${DIST_DIR}"/*)
shopt -u nullglob
if [ "${#files[@]}" -ne 4 ]; then
  echo "::error::Expected 4 files in ${DIST_DIR} (apk, aab, mapping, release notes), found ${#files[@]}."
  printf '  %s\n' "${files[@]}"
  exit 1
fi

notes=""
for f in "${files[@]}"; do
  case "$f" in *-release-notes.txt) notes="$f" ;; esac
done
if [ -z "$notes" ] || [ ! -s "$notes" ]; then
  echo "::error::No non-empty *-release-notes.txt in ${DIST_DIR}. The GitLab release body is the six-locale notes file, not CHANGELOG.md."
  exit 1
fi

# Same registry coordinates glab used, so links on older releases and links on
# this one address the same package.
links_json=""
for f in "${files[@]}"; do
  name="$(basename "$f")"
  url="${API}/packages/generic/${PACKAGE_NAME}/${TAG}/${name}"

  echo "Uploading ${name}"
  status="$(curl -sS -o /dev/null -w '%{http_code}' --request PUT "${auth[@]}" --upload-file "$f" "$url" || true)"
  case "$status" in
    200|201) ;;
    401|403)
      echo "::error::Upload of ${name} was rejected (HTTP ${status}). The token needs the 'api' scope."
      exit 1
      ;;
    *)
      echo "::error::Upload of ${name} failed (HTTP ${status})."
      exit 1
      ;;
  esac

  links_json="${links_json}${links_json:+,}$(
    python3 -c 'import json,sys; print(json.dumps({"name": sys.argv[1], "url": sys.argv[2], "link_type": "package"}))' \
      "$name" "$url"
  )"
done

body="$(python3 -c '
import json, sys
tag, notes_path, links = sys.argv[1], sys.argv[2], sys.argv[3]
with open(notes_path, encoding="utf-8") as fh:
    description = fh.read()
print(json.dumps({
    "name": tag,
    "tag_name": tag,
    "description": description,
    "assets": {"links": json.loads("[" + links + "]")},
}))
' "$TAG" "$notes" "$links_json")"

status="$(printf '%s' "$body" | curl -sS -o /tmp/gitlab-release.json -w '%{http_code}' \
  --request POST "${auth[@]}" \
  --header 'Content-Type: application/json' \
  --data-binary @- \
  "${API}/releases" || true)"

if [ "$status" != "201" ]; then
  echo "::error::Creating the GitLab Release for ${TAG} failed (HTTP ${status})."
  cat /tmp/gitlab-release.json || true
  exit 1
fi

echo "GitLab Release ${TAG} published with ${#files[@]} package assets."
echo "GitLab Release \`${TAG}\` published with ${#files[@]} package assets." >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
