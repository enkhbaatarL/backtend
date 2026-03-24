# Newsletter v2 Field Mapping (Canonical)

- version: `field-map-2026-03-12-r1`
- owner: BE
- policy: API/도메인/DB 명칭 매핑은 본 문서 단일 원본으로 관리한다.

## Mapping Table
| 영역 | API camelCase | 도메인 용어 | DB snake_case / physical | 비고 |
|---|---|---|---|---|
| Revision | `revisionId` | 리비전 식별자 | `nws_ltr_usr_rvsn_id` | revision PK |
| Revision | `revisionNo` | 리비전 변경시퀀스 | `rvsn_no` | 저장 성공 시 +1, 충돌 제어 |
| Revision | `editStatus` | 리비전 상태 | `rvsn_stat_cd` | `EDIT_DRAFT`, `EDIT_SAVED` |
| Revision | `savedAt` | 최종저장일시 | `fnsv_dtm` | save 시각 |
| Revision | `lastAutoSavedAt` | 최근임시저장일시 | `drft_strg_dtm` | draft 시각 |
| Revision Snapshot | N/A (internal) | 스냅샷 번호 | `snpsht_no` | 리비전 내 단조 증가 |
| Revision Snapshot | N/A (internal) | 스냅샷 기준 델타 | `base_dlta_no` | 복원 시작점 |
| Revision Delta | N/A (internal) | 델타 번호 | `dlta_no` | 리비전 내 단조 증가 |
| User | `userNo` | 사용자 식별자 | `usr_no` | 공통 사용자 키 |
| Revision Owner | `ownerUserNo` | 리비전 소유자 식별자 | `ownr_usr_no` | 소유권 가드 기준 |
| Subscription | `newsletterId` | 뉴스레터 마스터 식별자 | `nws_ltr_mstr_id` | 구독/리스트 기준 |
| Issue | `issueNo` | 발행 차수 | `issno` | 오리지널/리비전 기준 |

## Notes
- `revisionSeq`는 구용어이며 신규 문서/구현에서 사용하지 않는다.
- `snpsht_no`, `base_dlta_no`, `dlta_no`는 내부 저장 키이며 API 계약 필드로 직접 노출하지 않는다.
