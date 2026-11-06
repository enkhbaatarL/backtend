-- Event 웹 검증 결과 이력 테이블 설계안 (미적용)
-- 대상 DB 확인: MySQL 8.0.40, Event 테이블 collation=utf8mb4_bin.
-- 원본 Event: tmm_mi_mievent_m.mi_event_id (int unsigned).
-- 이 파일은 검토용이다. 애플리케이션 저장 로직/엔티티/마이그레이션에는 아직 반영하지 않았다.
--
-- 저장 단위
--   Event 1개 × 검증 실행 1회 = 1행. 재검증은 INSERT하여 이전 결과를 보존한다.
--   한 요청에 ID 1~5개를 보내면 request_id가 같은 행 1~5개를 한 트랜잭션으로 저장한다.
--   request_id는 Backend가 호출 전에 생성하는 UUID이며, request_order는 요청 ID 순서(1~5)다.
--   같은 결과의 저장 재시도는 동일 request_id를 사용한다. 새 웹 검색을 실행하는 재검증은 새 UUID를 사용한다.
--   UNIQUE 충돌 시 기존 결과를 무조건 덮어쓰지 않는다. 같은 요청인지 확인한 후 기존 기록을 사용한다.
--
-- 시간 및 스냅샷
--   checked_at: Agent의 최상위 checkedAt을 UTC로 정규화하여 저장. 완료/저장 시각과 다르다.
--   source_updated_at: Agent item.sourceUpdatedAt 원문. null/무시간대/offset 포함 값을 보존한다.
--   event_title: 검증 당시 제목. 이후 Event 제목이 바뀌어도 이력 화면에서 당시 제목을 표시한다.
--   result_json: 해당 Event의 Agent items 원소 전체. 다른 Event나 Backend BaseResponse는 넣지 않는다.
--     {miEventId,title,sourceUpdatedAt,status,reasonCode,review,links,checks}
--   상태/후기/제목/원문 JSON은 하나의 동일 Agent 결과에서 추출해 같은 INSERT로 저장한다.
--   crt_dtm/mod_dtm: 기존 시스템의 감사 시각 규칙을 따른다. Backend와 DB 세션 시간대를 맞춘다.
--     CURRENT_TIMESTAMP는 DB 세션 시간대이므로 자동으로 UTC라고 해석하지 않는다.
--
-- 사용자 및 삭제
--   UI 검증은 crt_usr_logn_id/mod_usr_logn_id에 인증된 요청 사용자의 로그인 ID를 저장한다.
--   최초 INSERT에서는 생성자=수정자. SYSTEM 기본값은 시스템 작업에만 사용한다.
--   감사 사용자 컬럼은 실제 Event 테이블과 동일하게 varchar(100)으로 제안한다.
--     향후 JPA 엔티티 구현 시 BaseEntity의 varchar(50) 매핑과 정합성을 맞춘다.
--   결과 본문은 이력으로 보존하며, 삭제는 del_yn='Y'와 수정자 갱신으로 처리한다.
--   Event 소프트 삭제 시 검증 이력은 보존한다. UI 조회 시 원본 Event의 삭제/접근 권한도 검사한다.
--   FK는 물리 삭제를 RESTRICT한다. 별도 승인 없는 연쇄 삭제를 수행하지 않는다.

CREATE TABLE `tmm_mi_mievent_verification_h` (
  `mi_event_verification_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'Event 검증 이력 PK',
  `mi_event_id` int unsigned NOT NULL COMMENT '검증한 Event ID',
  `request_id` char(36) NOT NULL COMMENT 'Backend 검증 요청 UUID; 한 번에 요청한 Event들의 묶음',
  `request_order` tinyint unsigned NOT NULL COMMENT '요청 ID 목록 내 순서(1~5)',
  `event_title` varchar(300) NOT NULL COMMENT '검증 당시 Event 제목 스냅샷',
  `status` varchar(20) NOT NULL COMMENT '검증 판정: pass/warning/fail (작업 실행 상태 아님)',
  `reason_code` varchar(40) NOT NULL COMMENT '상세 판정 코드: verified/review_required/no_evidence/search_error/verification_error/contradicted/outdated',
  `review` varchar(300) NOT NULL COMMENT '한국어 한 줄 검증 후기',
  `checked_at` datetime(6) NOT NULL COMMENT 'Agent 검증 기준 시각(UTC); 완료 시각 아님',
  `source_updated_at` varchar(40) DEFAULT NULL COMMENT 'Agent가 사용한 Event 수정 시각 원문; timezone 유무 보존',
  `result_json` json NOT NULL COMMENT '해당 Event의 Agent items 원소 전체; links/checks 및 스냅샷 포함',
  `crt_usr_logn_id` varchar(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성사용자로그인ID; UI 호출은 인증된 요청 사용자',
  `crt_dtm` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '결과 기록 생성일시',
  `mod_usr_logn_id` varchar(100) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정사용자로그인ID',
  `mod_dtm` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '결과 기록 수정일시',
  `del_yn` char(1) NOT NULL DEFAULT 'N' COMMENT '삭제여부(N/Y)',
  PRIMARY KEY (`mi_event_verification_id`),
  UNIQUE KEY `uq_event_verification_request_event` (`request_id`, `mi_event_id`),
  UNIQUE KEY `uq_event_verification_request_order` (`request_id`, `request_order`),
  KEY `idx_event_verification_latest` (`mi_event_id`, `del_yn`, `checked_at`, `mi_event_verification_id`),
  CONSTRAINT `fk_event_verification_event` FOREIGN KEY (`mi_event_id`)
    REFERENCES `tmm_mi_mievent_m` (`mi_event_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_event_verification_order` CHECK (`request_order` BETWEEN 1 AND 5),
  CONSTRAINT `ck_event_verification_deleted` CHECK (`del_yn` IN ('N', 'Y')),
  CONSTRAINT `ck_event_verification_result_object` CHECK (JSON_TYPE(`result_json`) = 'OBJECT'),
  CONSTRAINT `ck_event_verification_verdict` CHECK (
    (`status` = 'pass' AND `reason_code` = 'verified')
    OR (`status` = 'warning' AND `reason_code` IN ('review_required', 'no_evidence', 'search_error', 'verification_error'))
    OR (`status` = 'fail' AND `reason_code` IN ('contradicted', 'outdated'))
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Event 웹 검증 결과 이력';

-- 최신 유효 결과 조회 예시 (업무 API에서 Event 조회 권한을 먼저 검사)
-- SELECT * FROM tmm_mi_mievent_verification_h
-- WHERE mi_event_id = :eventId AND del_yn = 'N'
-- ORDER BY checked_at DESC, mi_event_verification_id DESC
-- LIMIT 1;
--
-- 같은 검증 요청의 결과 조회 예시
-- SELECT * FROM tmm_mi_mievent_verification_h
-- WHERE request_id = :requestId AND del_yn = 'N'
-- ORDER BY request_order;
--
-- 후속 구현 규칙
--   1. Agent HTTP 호출 중에는 DB 트랜잭션을 유지하지 않는다. 결과 검증 후 짧은 저장 트랜잭션을 연다.
--   2. HTTP 200의 search_error/verification_error도 유효한 항목별 warning 결과로 저장한다.
--      HTTP 404/422/5xx, 연결 실패, 응답 계약 위반처럼 항목별 결과가 없는 경우는 이 테이블에 가짜 판정을 넣지 않는다.
--      요청 전체 실패/진행 상태 이력이 필요해지면 별도 작업 테이블을 설계한다.
--   3. 원본 Event의 내용/mod_dtm/event_revision은 검증 결과 저장으로 변경하지 않는다.
--      이 테이블의 mod_dtm 자동 갱신은 원본 Event에 영향을 주지 않는다.
--   4. 늦게 완료된 과거 요청도 이력으로 INSERT한다. 최신 결과는 저장 순서가 아닌 checked_at 순서로 조회한다.
--   5. 검증 중/이후 Event가 변경됐다면 결과는 '검증 당시 내용'에 대한 이력이다.
--      source_updated_at과 현재 Event mod_dtm의 시간대 의미를 맞춰 비교하여 재검증 필요를 표시한다.
--      현재 Agent 응답에 원문 revision/hash가 없으므로 그 값을 임의로 만들거나 저장 시점 revision으로 대체하지 않는다.
--   6. 검증 결과가 없으면 이 테이블에 행이 없다. UI에서 '미검증'으로 표시하며 pass로 간주하지 않는다.
--   7. 저장 실패 시 UI에 저장 성공으로 안내하지 않는다. Agent 재호출과 결과 저장 재시도를 구분한다.
