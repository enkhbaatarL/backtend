#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:?TOKEN is required}"
NEWSLETTER_ID="${1:?newsletterId}"
FROM_ISSUE_NO="${2:?from issueNo}"
TO_ISSUE_NO="${3:?to issueNo}"

for ((issue_no=FROM_ISSUE_NO; issue_no<=TO_ISSUE_NO; issue_no++)); do
  echo "Replaying original snapshot: newsletterId=${NEWSLETTER_ID}, issueNo=${issue_no}"
  curl -sS -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/v1/mi/newsletter/${NEWSLETTER_ID}/${issue_no}/replay-original-snapshot"
  echo
done
