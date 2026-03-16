# DB 표준 단어 사전 사용 가이드 (Excel 2번째 시트 변환)

## 1. 소스

- 원본 파일: `/Users/hoduhomil/Downloads/downloadN737L162827/2.MI플랫폼_ERwin_컬럼영문명수식매핑.xlsx`
- 사용 시트: `2번째 시트 (데이터표준단어사전)`
- 변환 파일: [db-term-dictionary-sheet2.csv](/Users/hoduhomil/source/mip/backend/docs/conventions/db-term-dictionary-sheet2.csv)
- 변환 일자: 2026-03-12

## 2. 컬럼 구조

- `No`
- `단어종류`
- `표준단어명`
- `표준영문명`
- `영문의미`
- `설명`
- `참고사항`
- `등록자`
- `상태`

## 3. 설계 적용 규칙

- 테이블명/컬럼명 약어는 먼저 `표준단어명`과 `표준영문명`에서 조회한다.
- 사전에 없는 단어가 필요하고 사용이 적절하면 `db-term-dictionary-sheet2.csv`에 새 행으로 추가 후 반영한다.
- 같은 의미의 후보가 여러 개면 기존에 이미 사용 중인 `표준영문명`을 우선 사용한다.

## 4. 미등록 단어 추가 규칙

- 기존 CSV 컬럼 순서(`No, 단어종류, 표준단어명, 표준영문명, ...`)를 유지한다.
- 기존 행 수정/삭제 없이 마지막에 append만 수행한다.
- `No`는 마지막 번호 +1을 사용한다.
- 최소 입력 권장값:
  - `단어종류` (`단일단어` 또는 `복합단어`)
  - `표준단어명`
  - `표준영문명`
  - `참고사항`에 추가 사유/출처 기록
- 설계/리뷰 결과 보고 시 추가한 단어 목록을 함께 명시한다.

## 5. 빠른 조회 예시

```bash
rg "시장|선호|거래" docs/conventions/db-term-dictionary-sheet2.csv
rg ",TRDE," docs/conventions/db-term-dictionary-sheet2.csv
```

## 6. 운영 메모

- 이 CSV는 참조본이며, 기준 원본은 Excel 문서다.
- Excel이 개정되면 동일한 방식으로 2번째 시트를 재추출해 CSV를 갱신한다.
