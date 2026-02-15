#!/usr/bin/env bash
set -euo pipefail

# 사용법:
#   1) 파이프로 입력:
#      git status --porcelain=v1 | sed 's/^ M /modified:   /' | ./copy_modified_to_temp.sh
#      또는
#      cat list.txt | ./copy_modified_to_temp.sh
#
#   2) 직접 붙여넣기:
#      ./copy_modified_to_temp.sh <<'EOF'
#      modified:   src/...
#      modified:   src/...
#      EOF
#
# 옵션:
#   -d, --dest <dir>   대상 폴더 (기본: ./temp)
#   -r, --root <dir>   프로젝트 루트 (기본: 현재 디렉터리)

DEST="./temp"
ROOT="."

while [[ $# -gt 0 ]]; do
  case "$1" in
    -d|--dest)
      DEST="${2:?dest 경로가 필요합니다}"
      shift 2
      ;;
    -r|--root)
      ROOT="${2:?root 경로가 필요합니다}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [-d|--dest <dir>] [-r|--root <dir>]"
      exit 0
      ;;
    *)
      echo "알 수 없는 옵션: $1" >&2
      exit 2
      ;;
  esac
done

mkdir -p "$DEST"

# 입력에서 파일 경로만 추출:
# - 'modified:' 뒤의 경로를 가져옴
# - 빈 줄 제거
# - 중복 제거
# - (선택) 존재하는 파일만
paths="$(awk '
  {
    # "modified:" 포함 라인에서 뒤쪽 경로만 뽑기
    if ($0 ~ /modified:[[:space:]]+/) {
      sub(/^[[:space:]]*modified:[[:space:]]+/, "", $0)
      if (length($0) > 0) print $0
    }
  }
' | sed '/^[[:space:]]*$/d' | sort -u)"

if [[ -z "${paths}" ]]; then
  echo "복사할 경로를 찾지 못했습니다. (입력에 'modified:' 라인이 있는지 확인)" >&2
  exit 1
fi

# 임시 files-from 생성 (공백/특수문자 경로 대응용: 줄 단위 전달)
tmp_list="$(mktemp)"
trap 'rm -f "$tmp_list"' EXIT
printf "%s\n" "${paths}" > "$tmp_list"

# 핵심:
#  -a            : 권한/시간 등 보존(가능한 범위)
#  --relative(-R): 전달된 경로를 상대 경로로 유지해서 DEST 아래에 그대로 생성
#  --files-from  : 목록 기반 복사
#  --prune-empty-dirs : 빈 디렉터리 생성 방지
rsync -a --relative --prune-empty-dirs \
  --files-from="$tmp_list" \
  "$ROOT"/ \
  "$DEST"/

echo "완료: $(wc -l < "$tmp_list" | tr -d ' ')개 파일을 '$DEST'로 경로 유지 복사했습니다."
