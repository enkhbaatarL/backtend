# Newsletter v2 문서 거버넌스

## 목적
- Newsletter v2 관련 문서에서 계약 사실(Source of Truth) 중복/충돌을 방지한다.
- 구현 착수 전 문서 정합성 점검 기준을 강제한다.

## SoT 원칙
- API 계약 사실(엔드포인트, 필드, 타입, 응답/에러 envelope)은 `docs/newsletter-v2-openapi.yaml`에만 고정한다.
- 물리 스키마/DBA 반영 사실은 canonical DDL 문서군에만 고정한다.
  - canonical DDL 인덱스: `docs/newsletter-v2-ddl-canonical-index.md`
- Living/Confluence/Thread는 계약 사실을 재서술하지 않고 링크 참조만 유지한다.

## 문서별 책임
- Living Doc (`docs/newsletter-v2-design.md`)
  - 도메인 규칙, 정책, 결정 배경, 단계별 운영 가정
  - 금지: OpenAPI/DDL 필드 스키마를 본문에 재정의
- Confluence 요약 (`docs/newsletter-v2-design-confluence.md`)
  - 의사결정 공유 요약, 합의 전달
  - 금지: 계약 세부 스키마 재정의
- Thread 문서 (`docs/newsletter-v2-thread-*.md`)
  - 실행 상태(게이트/결정/테스트/릴리즈 메모)
  - 금지: API/DDL 스키마 재서술

## Contract Sync Checklist (필수)
- OpenAPI version
- Canonical DDL version
- Last synced at (KST)
- Sync owner

위 체크리스트는 Living/Confluence/Thread 문서에 공통으로 포함한다.

## 충돌 금지 룰
- Decision Log에 `확정`된 항목은 Open Questions에서 `OPEN BLOCKER`로 동시에 둘 수 없다.
- 위반 시 문서 병합 금지.

## 변경 절차
1. OpenAPI 또는 canonical DDL을 먼저 수정
2. Living/Confluence/Thread의 참조 링크와 체크리스트 동기화
3. 스레드 게이트 상태와 결정/오픈이슈 충돌 여부 확인
4. Work note 기록

## PR 체크 규칙
- PR에는 아래 확인 항목이 포함되어야 한다.
  - Contract Sync Checklist 갱신
  - OpenAPI/DDL 링크 최신화
  - Thread 문서에 계약 스키마 재서술 없음
  - `확정` vs `OPEN BLOCKER` 충돌 없음
