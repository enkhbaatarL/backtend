# Newsletter v2 설계 요약 (Confluence 공유용)

## 1. 문서 목적
- Newsletter v2 설계 합의안을 기획/프론트/백엔드 공용 기준으로 공유
- 본 문서는 Confluence 복붙용 요약본
- 상세 Living Doc: `docs/newsletter-v2-design.md`

## Contract Sync Checklist
- OpenAPI version: `1.0.0` (`docs/newsletter-v2-openapi.yaml`)
- Canonical DDL version: `ddl-canonical-2026-03-12-r2` (`docs/newsletter-v2-ddl-canonical-index.md`)
- Last synced at (KST): `2026-03-12 21:40`
- Sync owner: `BE`

## 1-1. 문서 사용 규칙 (요약본)
- 본 문서는 계약 원본(SoT)이 아니라 공유용 요약본이다.
- 상태/전이/에러코드 원본: `docs/newsletter-v2-design.md`
- API 스키마 원본: `docs/newsletter-v2-openapi.yaml`
- DDL 적용 절차 원본: `docs/newsletter-v2-ddl-canonical-index.md` 및 DBA 최종 반영본
- Thread 문서는 실행 단위의 검증/게이트 상태만 기록하고, 계약 본문은 중복 정의하지 않는다.
- 상세 거버넌스 규칙: `docs/conventions/newsletter-doc-governance.md`

## 1-2. 용어 표준 (Canonical)
- 명칭 매핑 원본: `docs/newsletter-v2-field-mapping.md`
- 요약본에서는 매핑표를 재정의하지 않고 링크만 유지한다.

## 2. 배경 및 방향
- 기존: 템플릿/타 테이블 조합 기반 상세 조회
- 변경: 발행 시점 원본 스냅샷 저장 + 사용자 개인 수정본(Revision) 및 수정 이력(RevisionSnapshot) 저장/조회

## 3. 핵심 요구사항
- 오리지널 발행 상태 관리
- 사용자 개인 수정(임시저장/최종저장)
- 구독/구독해지
- 오리지널 구독자 메일 발송은 배치 처리
- 사용자 지정 수신자 메일 발송(편집본 기준, 필요 시 이미지 포함)
- 템플릿 구성 확장: 기존 제목/메인/참고 영역 기반 + Section/Block 계층형 편집 모델 도입

## 4. 상태 모델

### 4.1 Originals (`NewsletterPublishHistory.publishStatusCode` 재사용)
| 상태 | 의미 |
|---|---|
| `PUBLISHING` | 생성/발행 처리 중 |
| `PUBLISHED` | 발행 완료 (회차별 전체 조회 가능) |
| `SUSPENDED` | 관리자 숨김/중단 (기본 목록 제외) |

### 4.2 Revisions (신규)
| 상태 | 의미 |
|---|---|
| `EDIT_DRAFT` | 수정 중/임시저장 상태 |
| `EDIT_SAVED` | 최종 저장 상태 (메일 발송 가능) |
| 임시저장 정책 | 사용자별/이슈별 리비전 1건 유지, 임시저장은 최신 1개만 덮어쓰기 |

### 4-3. 콘텐츠 템플릿 모델 확장
- 기존 구성
  - 제목 부분 / 메인 영역 / 참고 영역으로 나뉘어 렌더링
  - 참고 영역은 `tmd_nws_ltr_tmplt.nws_ltr_tmplt_id`와 `elementType` 기반 차트/표 동적 렌더링
- 신규 구성
  - `Section`(상위) + `Block`(하위) 모델을 기본 단위로 운영
  - Section 타입: `FIXED`, `GENERAL`, `SPECIALIZED`
  - Block 타입: `GENERAL`, `SPECIALIZED`
  - `SPECIALIZED`는 `target`으로 고객사 바인딩(SKE, SKHy 등)
  - 모든 Section/Block은 Collapse/Expand 지원
  - 필수값 미입력은 Live update 경고 처리
  - 섹션 추가/블록 추가/수정/삭제를 통해 구조 변경 가능(운영 예외 정책 적용)
  - Section/Block Tool
    - `Order`: 섹션·블록 순서 지정
    - `Visibility`: 노출/비노출 여부 토글
    - `Plot on Chart`: 차트 위 변곡점 표시 여부 토글
    - `Delete`: 항목 삭제

### 4-4. Section/Block JSON 스키마 핵심(요약)
- 리비전 콘텐츠 최상위는 `sections` 배열
- Section: `id`, `type(FIXED|GENERAL|SPECIALIZED)`, `title`, `order`, `visibility`, `isCollapsed`, `blocks`
- Block: `id`, `type`, `format(GENERAL|SPECIALIZED)`, `title`, `order`, `visibility`, `isCollapsed`, `plotOnChart`, `content`
- `SPECIALIZED`는 `target` 필수
- `plotOnChart`는 CHART 블록에서만 유효
- `EDIT_DRAFT`: 필수값 경고 허용, `EDIT_SAVED`: 필수값 유효성 통과 필요

### 10.1-1. 기존 템플릿 이관 가이드
- `고정형` 섹션은 기존 제목/메인/참고 영역 매핑과 병행 유지
- `참고 영역`의 차트/표는 `tmd_nws_ltr_tmplt`의 타입 메타데이터를 Block 메타로 매핑해 점진 이관

## 5. 상태 전이

### 5.1 Originals
- `PUBLISHING -> PUBLISHED`
- `PUBLISHED -> SUSPENDED` (선택적 관리자 액션)
- 운영 정책에 따라 `SUSPENDED -> PUBLISHED` 복구 가능

### 5.2 Revisions
- `None -> EDIT_DRAFT` (최초 저장 호출 시 리비전 자동 생성)
- `EDIT_DRAFT -> EDIT_DRAFT` (임시저장 반복)
- `EDIT_DRAFT -> EDIT_SAVED` (최종 저장)
- `EDIT_SAVED -> EDIT_DRAFT` (재수정 시작)

### 5-1. 스레드 1 확정표 (도메인 기준)

#### Originals
| From | To | 조건 | 권한 |
|---|---|---|---|
| null(생성 전) | PUBLISHING | 발행 생성/등록 | 시스템/배치 |
| PUBLISHING | PUBLISHED | 발행 완료 | 시스템/배치 |
| PUBLISHED | SUSPENDED | 운영상 비노출 처리 | ADMIN |
| SUSPENDED | PUBLISHED | 운영 복구 | ADMIN |

#### Revisions
| From | To | 조건 | 권한 |
|---|---|---|---|
| None | EDIT_DRAFT | 최초 임시저장/최종저장 호출 시 리비전 생성 | 소유자 |
| EDIT_DRAFT | EDIT_DRAFT | 임시저장 반복 | 소유자 |
| EDIT_DRAFT | EDIT_SAVED | 최종 저장 | 소유자 |
| EDIT_SAVED | EDIT_DRAFT | 재수정 시작(임시저장) | 소유자 |

## 6. 핵심 규칙 (확정)
- 리비전 소유권: 본인 외 조회/수정/발송 금지
- 상태 가드: 메일 발송은 `EDIT_SAVED`에서만 허용
- 충돌 제어: `revisionNo`(변경시퀀스) 기반 낙관적 락 (`409 Conflict`)
- 멱등 생성: `(nws_ltr_org_iss_id, ownr_usr_no)` 유니크로 1인 1리비전 보장
- 오리지널 구독 메일 발송: `PUBLISHED` 상태에서 오리지널 메일 발송
- 오리지널 구독 메일 발송은 배치 처리로 운영

## 7. 도메인 일관성 경계
| 경계 | 책임 |
|---|---|
| `NewsletterOriginal` | 회차 발행 상태, 원본 콘텐츠 보존, 발행 시각 |
| `NewsletterRevision` | 개인 수정 콘텐츠, 임시/최종 저장 상태, 버전/수정 이력 |
| `NewsletterSubscription` | 사용자-뉴스레터마스터 구독 여부 관리 |

## 8. 비즈니스 흐름

### 8.1 오리지널 발행
1. Airflow가 오리지널 생성/발행
2. 상태 전이: `PUBLISHING -> PUBLISHED`
3. 구독자 대상 배치 메일 발송 (오리지널 기준)

### 8.2 사용자 개인 수정/발송
1. 오리지널 기반 화면 진입(리비전 미생성 상태는 프론트 임시 편집 상태로 구성)
2. 임시저장 반복 (`EDIT_DRAFT`)
3. 최종저장 (`EDIT_SAVED`)
4. 메일보내기 클릭 후 수신자 이메일 입력
5. send-mail payload에 메일 본문/이미지 포함 후 발송

## 9. 데이터 스키마 초안

### 9.1 `newsletter_issue_original` (신규 권장)
- `id` (PK)
- `nws_ltr_mstr_id`
- `issno`
- `pub_stat_cd`
- `pub_dtm`
- `org_snpsht_json`
- `schm_ver`
- `created_at`, `updated_at`

### 9.2 `newsletter_user_revision`
- `id` (PK)
- `nws_ltr_org_iss_id` (FK)
- `ownr_usr_no`
- `rvsn_stat_cd` (`EDIT_DRAFT`, `EDIT_SAVED`)
- `rvsn_no` (변경시퀀스)
- `drft_strg_dtm`
- `fnsv_dtm`
- `created_at`, `updated_at`, `is_deleted`
- 제약: `UNIQUE(nws_ltr_org_iss_id, ownr_usr_no)`
- 본문은 `newsletter_user_revision`에 단일 컬럼으로 고정 저장하지 않고, `snapshot + delta` 조합으로 복원한다.

### 9.3 `newsletter_user_revision_snapshot`
- `id` (PK)
- `nws_ltr_usr_rvsn_id` (FK)
- `snpsht_no`
- `base_dlta_no`
- `snpsht_json`
- `change_summary` (optional)
- `edited_by`
- `edited_at`
- 제약: `UNIQUE(nws_ltr_usr_rvsn_id, snpsht_no)`
- 이력은 감사 목적 저장용이지만 임시저장 본문은 `/revisions/draft` 조회 기준 최신 1건 단위로 본다.

### 9.4 `newsletter_subscription`
- `id` (PK)
- `nws_ltr_mstr_id` (FK)
- `userNo` (physical: `usr_no`)
- `is_subscribed`
- `subscribed_at`
- `unsubscribed_at`
- `created_at`, `updated_at`, `is_deleted`
- 제약: `UNIQUE(nws_ltr_mstr_id, userNo)` (physical: `(nws_ltr_mstr_id, usr_no)`)

## 10. API 설계 초안 (v2)
- 공통 응답 규약: 성공/실패 모두 `BaseResponse` envelope(`status`, `code`, `message`, `data`)를 사용한다.
- 성공 시 도메인 payload는 `data`에 담아 전달한다.
- 실패 시 `ResultCode`는 `code` 필드로 전달하며, 별도 `errorCode` 단독 응답은 사용하지 않는다.

### 10.1 Originals
| Method | Path | 설명 |
|---|---|---|
 | GET | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}` | 원본 or 편집본 상세 조회 (편집본 존재 시 편집본 반환), 응답 동기화 항목 포함 |

  - 상세조회는 리비전이 존재할 때 `revisionId`를 포함한다.
  - `/revisions/draft`, `/revisions/save`는 revisionId 미존재 상태에서 자동 생성한다.
  - 리비전 존재 시 `/revisions/{revisionId}/changes...` API에 사용한다.
  - 응답 필드는 `source`를 추가하여 반환 본문 유형을 명시한다. (`ORIGINAL` or `REVISION`)
  - `source`가 `REVISION`이면 편집본 반환, `ORIGINAL`이면 원본 반환.
| POST | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/send-mail` | 편집본 메일 발송 (`EDIT_SAVED` 가드, `recipientEmails` 미입력 시 유효 수신자 없음) |

### 10.2 Revisions
| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft` | 임시저장 최신본 조회 (hasDraft=true일 때 호출) |
| PUT | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft` | 임시저장 (`revisionNo` 선택, 최초 호출 가능, revisionId 미존재 시 자동 생성) |
| PUT | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/save` | 최종저장 (`revisionNo` 선택, 최초 호출 가능, revisionId 미존재 시 자동 생성 + EDIT_SAVED 전환) |
| GET | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/{revisionId}/changes` | 수정 이력 목록 조회 |
| GET | `/api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/{revisionId}/changes/{seq}` | 특정 수정 이력 조회 |

### 10-2. revisions 응답 규칙 정리(프론트 동기화)
- `originalSnapshot/latestSnapshot`의 `revisionNo=0`은 원본 스냅샷 기준값(sentinel)이며, 리비전 저장 버전값이 아니다.
- `baseSnapshot.seq`는 `fromSeq`로 간주
- 응답엔 `fromSeq`, `toSeq`도 함께 제공
- 프론트는 `baseSnapshot`에서 `histories`를 `seq` 오름차순으로 적용해 최종 상태 재구성
- `baseSnapshot`은 구간 시작 상태, `histories`는 구간 변경분(`fromSeq+1..toSeq`)으로 정의
  - `GET /revisions/{revisionId}/changes`는 페이징 UI가 없으므로 **최대 20건**만 반환(최신 seq 내림차순).
- 상세조회 응답의 `hasDraft=true`면 임시저장본 조회를 위해 `GET /revisions/draft` 호출
- 임시저장 반영 정책: `PUT /revisions/draft` 반복 호출 시 저장본은 덮어써서 단일 최신 임시저장본만 유지한다.
- `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}` 응답 동기화 필드
  - `source`
  - `hasDraft`
  - `originalSnapshot`
  - `latestSnapshot`
  - `deltaChanges`
  - `deltaCount`

### 10-2. 스냅샷 생성 정책(클라이언트 결정)
- 상세조회 응답의 `deltaCount`를 기준으로 프론트에서 임계치 정책 적용
- 권장 초기 정책: `deltaCount > 8`이면 `PUT /revisions/save`에 `snapshotPayload`를 함께 전송
- 운영 조정 범위: `6~10` (상세조회 리플레이 `p95` 지표 기반)
- 백엔드는 `snapshotPayload`가 있을 때만 스냅샷 생성/갱신

### 10-3. revisions 저장 규칙 (deltas 단독)
- `POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/histories`는 제거한다.
- `PUT /revisions/draft`, `PUT /revisions/save`의 요청 파라미터 형태는 동일(`deltas`).
- 요청 endpoint(`draft/save`)로 저장유형을 구분한다.
- 양 endpoint 모두 `deltas` 누적 append 방식으로 적용한다.
- `PUT .../revisions/draft`: 임시 저장 전용, 현재 콘텐츠 반영
- `PUT .../revisions/save`: 최종 저장 전용, 상태 전이(`EDIT_DRAFT -> EDIT_SAVED`)
- `snapshotPayload`는 최종 저장 요청에서만 선택 값으로 전달 가능하며, 전달 시 snapshot 저장/갱신 수행
- 스냅샷 생성은 클라이언트가 `snapshotPayload` 포함 여부로 결정

### 10.3 Subscriptions
| Method | Path | 설명 |
|---|---|---|
| PUT | `/api/v1/mi/newsletter/{newsletterId}/subscription` | 구독 (멱등) |
| DELETE | `/api/v1/mi/newsletter/{newsletterId}/subscription` | 구독해지 (멱등) |
| GET | `/api/v1/mi/newsletter/{newsletterId}/subscription` | 내 구독 상태 조회 (권장) |

## 11. 에러 코드 초안
| 코드 | HTTP | 설명 |
|---|---|---|
| `REVISION_FORBIDDEN` | 403 | 타인 리비전 접근/수정/발송 |
| `REVISION_NOT_FOUND` | 404 | 리비전 없음 |
| `REVISION_VERSION_CONFLICT` | 409 | 낙관적 락 충돌 |
| `REVISION_INVALID_STATE` | 422 | `EDIT_SAVED` 전 메일 호출 |
| `NEWSLETTER_NOT_FOUND` | 404 | 원본/마스터 없음 |
| `INVALID_RECIPIENT_EMAIL` | 400 | 수신자 이메일 포맷/입력 오류 (`recipientEmails` 유효값 1건 미만) |

- 에러 응답 본문 형식: `BaseResponse` (`status=false`, `code=<ResultCode>`, `message=<설명>`, `data=null`)

### 10-0. 리비전 저장/조회 핵심 플로우

#### 1) draft 저장 (최초 저장 포함)

- 진입 경로 A: 상세조회 선행 없음
  - 클라이언트: `PUT /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft`
  - 서버: 사용자-이슈 기준 리비전이 없으면 생성 후 저장
  - 충돌 정책: `revisionNo` 있으면 일치 검증 후 불일치시 `409`

- 진입 경로 B: 상세조회 선행
  - 클라이언트: `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}`
  - 서버: `revisionId` 포함 응답
  - 클라이언트: `PUT /revisions/draft` 호출 (동일 규칙)

- 응답 해석:
  - `hasDraft`는 임시저장 존재 여부
  - `revisionNo`는 선택값(미입력 시 충돌검사 생략)

#### 2) save 최종저장

- 클라이언트: `PUT /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/save`
- 동작:
  - 리비전 미존재 시 자동 생성 후 `EDIT_SAVED` 전환
  - `revisionNo`가 있을 때만 충돌 검증
  - `snapshotPayload`가 있으면 스냅샷 저장

#### 3) 조회/발송 경로

- 임시저장본 조회: `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/draft`
- 이력 목록: `GET /api/v1/mi/newsletter/{newsletterId}/{issueNo}/revisions/{revisionId}/changes`
- 수정본 존재 시 메일은 편집본 기준으로 발송: `POST /send-mail`
- 발송 실패 가드: 최종저장 상태가 아니면 `EDIT_SAVED` 가드 위반 처리

## 11-1. 히스토리 저장 충돌 정책 (단일 사용자 가정)
- `revisionNo`는 선택 값이다.
- `revisionNo` 미입력: 충돌 검사 없이 저장한다.
- `revisionNo` 입력:
  - 리비전 존재 시 현재 `revisionNo`와 일치해야 하며, 불일치 시 `409 REVISION_VERSION_CONFLICT`.
  - 리비전 미존재 시 신규 생성 처리(`1` 시작).
- 기본 권고: 저장 API 호출 전에 상세 조회로 `revisionId`를 획득해 저장/조회/히스토리 요청에 재사용한다.
- 다중 탭/중복 요청은 필요 시 `clientMutationId`로 보완 운영한다.

## 11-2. 리비전 모델 확정안 (요약)
- 본문 저장 모델: `content_json` 단일 컬럼 미사용, `snapshot + delta` 복원 모델
- `revisionNo`(`rvsn_no`)는 저장 성공 시 증가, 최초 저장 결과는 `1`
- 스냅샷 시점 키는 `snpsht_no` + `base_dlta_no` 조합 사용
- 상세 매핑/물리명은 `docs/newsletter-v2-field-mapping.md`를 원본으로 참조

## 12. 구현 단계
1. Phase 0: API/상태/JSON 스키마 확정
2. Phase 1: DDL + 엔티티/리포지토리
3. Phase 2: 원본 스냅샷 저장 + 원본 상세 v2 조회
4. Phase 3: 리비전/리비전 + 낙관적 락 + 소유권 가드
5. Phase 4: 구독/구독해지 + 오리지널 구독 메일
6. Phase 5: 사용자 지정 수신자 메일 (첨부 이미지 포함)
7. Phase 6: 전환/운영 안정화 + 기존 상세 조합 로직 축소

## 13. 미확정/추가 결정 필요
- 리비전 콘텐츠 블록 타입/필수 필드/길이 제한
- 메일 첨부(이미지) 정책(해상도, 동기/비동기, 렌더링/생성 방식)
- 메일 정책(첨부/inline, 수신자 수 제한, 실패 재시도)
- 과거 발행본 백필 범위
- 히스토리 저장 정책(확정)
  - `PUT /revisions/save`는 `deltas`를 유지.
  - 최종저장 시점에 `snapshotPayload`가 있으면 스냅샷 저장.
  - `revision_type`은 API 응답에서 구분값으로 유지(AUTOSAVE/FINAL) 가능.

## 13-1. 스레드 5 결정안 (2026-03-04)
- 메일 요청은 `multipart/form-data`를 사용하고 `subject`, `htmlBody`, `images`, `recipientEmails`를 받는다.
- `recipientEmails`는 필수 입력이다. (multipart 반복 키)
- `scheduledAt`은 ISO-8601 문자열(예: `2026-03-07T10:30:00+09:00`, `2026-03-07T10:30:00`)을 받는다.
- 편집본 발송: `recipientEmails` 미입력 시 즉시 실패한다.
- 유효 주소가 1개도 없으면 `INVALID_RECIPIENT_EMAIL`(400).
- `recipientEmails` 전달 시 유효한 주소만 사용하며, 유효 주소가 1개도 없으면 400 처리(`INVALID_RECIPIENT_EMAIL`).
- `cid`는 `images` 각 파일명에서 확장자를 제거한 값으로 자동 생성한다. (파일명 UUID 권장)
- 중복 파일명은 `-1`, `-2` suffix를 붙여 고유 CID로 보정한다.
- 생성된 `cid`는 허용 문자만 사용하고, `htmlBody`의 `cid:` 참조와 양방향 일치 검증한다.
- 인라인 메일 구성 시 `setText(htmlBody, true)` 후 `addInline` 순서를 강제한다.
- 메일 재시도 시 매 시도마다 `MimeMessage`를 새로 생성한다.
- `subject`, `htmlBody`, `recipientEmails`, `scheduledAt`는 URL query가 아닌 `multipart/form-data` 본문으로 전달한다.
- `send-mail` 요청은 flat multipart 필드 구조를 사용한다. (`subject`, `htmlBody`, `recipientEmails`, `scheduledAt`, `images`)
- JSON `request` 파트는 사용하지 않는다.

### send-mail 요청 시그니처(초안)
- `POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/send-mail`
  - `subject` (required)
  - `htmlBody` (required)
  - `recipientEmails` (required, 반복 키)
  - `scheduledAt` (optional, ISO-8601)
  - `images` (optional, 반복 키)

## 발송 이력 / 예약 발송 확장
- 오리지널/에디션 발송 요청은 모두 `newsletter_send_history` 레코드 생성 후 처리한다.
- 오리지널 구독 발송도 수신자 기준(`recipient_type=SUBSCRIBER`)으로 이력을 생성한다.
- 사용자 수동 발송도 수신자 기준(`recipient_type=MANUAL`)으로 이력을 생성한다.
- Airflow 등 외부 트리거 기반 발송은 발송 이력과 별도로 `trigger 멱등/상태` 테이블을 같이 설계한다.
- `scheduledAt`을 받아 미래 시각이면 예약 상태로 생성, 과거/미입력은 즉시 발송 대상으로 취급한다.
- 스케줄러는 `scheduled_at <= NOW` 및 `status in (PENDING, SCHEDULED)` 기준으로 이력을 처리한다.
- 상태 전이 기본값은 `PENDING -> PROCESSING -> SENT / FAILED`이다.
- 실패 시 `retry_count`를 증가시키고 재시도 정책을 적용한다.
- 예약 건은 실행 전 `CANCELLED`로 전환할 수 있다.

### 추천 테이블 `newsletter_send_trigger` (신규)
- 목적: trigger-mail API 중복 호출 방지, 비동기 처리 상태 추적
- `id` PK
- `idempotency_key` (UNIQUE)
- `newsletter_id`, `issno`
- `status` (`RECEIVED`, `PROCESSING`, `SUCCEEDED`, `FAILED`)
- `request_hash` (optional), `error_message` (optional)
- `requested_by` (SYSTEM/API role 식별값)
- `created_at`, `updated_at`

### trigger-mail API 처리 규칙
- API 진입 시 `idempotency_key` 기준으로 선검증한다.
- 동일 key가 `SUCCEEDED` 또는 `PROCESSING`이면 중복 요청으로 간주하고 재발송하지 않는다.
- 최초 key만 비동기 발송 작업으로 전달하고 상태를 갱신한다.
- 결론: `newsletter_send_history`는 "수신자/발송 결과 이력", `newsletter_send_trigger`는 "트리거 멱등/실행 상태" 책임으로 분리한다.

### 추천 테이블 `newsletter_send_history`
- `id` PK
- `target_type` (`ORIGINAL`, `EDITION`)
- `target_id` (FK)
- `requestUserNo`
- `recipient_type` (`SUBSCRIBER`, `MANUAL`)
- `recipient_email`
- `status` (`PENDING`, `SCHEDULED`, `PROCESSING`, `SENT`, `FAILED`, `CANCELLED`)
- `subject`, `message`
- `channel` (`EMAIL`)
- `scheduled_at`, `requested_at`, `started_at`, `finished_at`
- `retry_count`, `max_retry_count`, `last_error_code`, `last_error_message`
- `is_deleted`, `created_at`, `updated_at`

### send-mail API 확장
- `send-mail` 요청에 `scheduledAt`(optional) 추가
- 응답에 `sendHistoryId` 또는 `sendHistoryGroupId` 반환
- `recipientEmails`가 여러 개여도 메일 요청 1회로 다중 수신자 전송이 가능하다. (수신자 공개 여부는 To/Bcc 정책으로 분리)
- 후속 구현 메모: `MiNewsletterMailService.java`에 메일 발송 처리와 `newsletter_send_history` 이력 저장을 통합하는 로직을 추가한다.

### 발송 요청 식별/수신자 정책(최종 확정)
- 요청자 식별 필드는 `requestUserNo`를 사용한다. (시스템 `userNo`)
- 오리지널 발송: 구독자 대상(`recipient_type=SUBSCRIBER`)으로 내부 수신자 목록을 확정하여 발송 이력을 생성한다.
- 사용자 편집본 발송: 수동 발송만 허용하며, 수신자 목록은 항상 `recipientEmails` 입력으로 받는다.
- 사용자 편집본은 `POST /api/v1/mi/newsletter/{newsletterId}/{issueNo}/send-mail`를 통해 발송한다. 해당 요청은 내부에서 사용자 최신 리비전(또는 요청 시 지정한 revision)을 대상 revision으로 사용하며, `recipientEmails`는 필수 입력이다. (빈 목록이면 `INVALID_RECIPIENT_EMAIL`)

### API(예정)
- `POST /api/v2/mi/newsletters/{newsletterId}/issues/{issueNo}/send-mail`
- `GET /api/v2/mi/newsletters/{newsletterId}/issues/{issueNo}/send-histories`
- `GET /api/v2/mi/newsletters/send-histories/{sendHistoryId}`
- `POST /api/v2/mi/newsletters/send-histories/{sendHistoryId}/cancel`
- `send-mail` 요청 필드: `recipientEmails`, `subject`, `message(optional)`, `scheduledAt(optional)`

## 14. 스레드 운영 가이드
아래 단위로 스레드를 분리해 논의와 산출물을 관리한다.
- 실행 템플릿: `docs/newsletter-v2-thread-template.md`

### 스레드 1: 도메인/상태 확정
- 범위: 상태 모델, 상태 전이, 권한 가드, 용어
- 산출물: 상태전이표 확정본
- 실행 문서: `docs/newsletter-v2-thread-1.md`

### 스레드 2: 데이터 모델/마이그레이션
- 범위: 테이블 구조, 키/인덱스/유니크, 백필 전략
- 산출물: DDL 초안, 마이그레이션 계획서

### 스레드 3: API 계약
- 범위: 엔드포인트, request/response, 에러코드, 멱등/락 규칙
- 산출물: OpenAPI 초안

### 스레드 4: 상세 콘텐츠 스키마
- 범위: 리비전 콘텐츠 블록 타입, 필수값, 검증 규칙
- 산출물: JSON Schema + 샘플 payload

### 스레드 5: 메일/이미지 정책
- 범위: 오리지널 구독 메일, 리비전 메일, PNG 생성 정책
- 산출물: 발송/실패처리/성능 기준 문서

### 스레드 6: 구현/테스트/릴리즈
- 범위: Phase별 구현 순서, 테스트 계획, 기능 플래그/전환
- 산출물: 실행 계획 + QA 체크리스트

## 15. 변경 이력
- 2026-03-03: Confluence 공유용 요약본 생성
- 2026-03-04: 스레드 5 메일 정책 구체화 (선택 수신자 + send-mail 시그니처 반영)
