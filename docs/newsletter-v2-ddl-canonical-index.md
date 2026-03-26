# Newsletter v2 Canonical DDL Index

- version: `ddl-canonical-2026-03-26-r4`
- last updated at (KST): `2026-03-26`
- owner: BE

## Canonical DDL set
- `docs/newsletter-template-v2-ddl-draft.sql`
- `docs/newsletter-template-v2-data-src-spec-alter.sql`
- `docs/newsletter-issue-original-ddl-draft.sql`
- `docs/newsletter-issue-state-ddl-draft.sql`
- `docs/newsletter-user-revision-ddl-draft.sql`
- `docs/newsletter-user-revision-delta-ddl-draft.sql`
- `docs/newsletter-user-revision-snapshot-ddl-draft.sql`
- `docs/newsletter-subscription-ddl-draft.sql`
- `docs/newsletter-v2-create-tables-only.sql`

## Note
- Newsletter v2 물리 스키마 사실은 위 문서군에만 고정한다.
- Living/Confluence/Thread 문서는 본 인덱스와 OpenAPI를 링크 참조만 유지한다.
- `newsletter-issue-state-ddl-draft.sql`은 issue section/block state의 canonical DDL 초안이며, published snapshot read 경로를 대체하지 않는다.
- `newsletter-v2-create-tables-only.sql`은 신규 환경 초기 구축용 통합 DDL이며, 부모 테이블 ALTER 포함.
