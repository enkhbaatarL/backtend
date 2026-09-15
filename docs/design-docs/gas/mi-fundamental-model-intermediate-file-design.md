# MI Fundamental Model 실행 중간파일 연결·노출 설계

## Document Control

- status: Approved
- canonical for: 수급모델 실행 중간파일의 식별/연결 규칙, 상세 조회 노출 범위, 모델(pod) 적재 계약
- owner: BE
- source documents:
  - `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md`
  - `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md`
  - `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md`
  - `docs/design-docs/gas/mi-fundamental-model-agent-model-interface-draft.md`
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md`
- depends on:
  - `tmd_mdl_scnrcaseh.run_seq`
  - `tcm_filel.file_rel_obj_spt_cd`, `tcm_filel.file_rel_obj_dstg_no`
  - 공통 파일 다운로드 API (`POST /api/v1/file-management/download/file`, `POST /api/v1/file-management/download`)
- supersedes:
  - `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md` §Detail Query Recommendation 의 "제외 범위: intermediate file metadata" 문장 (이 설계가 확정되면 supersede)
- last synced: 2026-09-07
- open questions: 아래 §9 참조

## 1. 목적과 범위

### 목적

수급모델 실행 중 모델 측이 생성·업로드하는 **중간파일(n개)** 을 기존 output 파일과 구분해 저장하고,
`GET /api/v1/mi/fundamental-model/cases/{caseId}` 상세 조회에서 **파일명 목록으로** 제공하는 식별 규칙·계약·변경 범위를 확정한다.

실제 다운로드 기능은 이번 범위에 넣지 않는다. 현재 상세 응답이 `fileNm` 만 노출하는 것은 input/output 을 포함한
**의도된 보안 결정**이며, 중간파일도 같은 규칙을 따른다. 다운로드 활성화(§6, TODO)는 별도 결정 후 진행한다.

### 범위 안

- 중간파일을 output 파일과 구분하는 식별 방식 결정
- 공통코드/스키마 변경 범위(delta)
- 상세 조회 응답 계약 변경 (파일명 목록)
- 모델(pod)이 `tcm_filel` 에 직접 INSERT 할 때 지켜야 하는 필수 컬럼 계약
- 영향받는 canonical 문서와 코드 변경 목록

### 범위 밖

- 중간파일 내용 파싱/결과 수치 적재 (`mi-fundamental-model-output-result-persistence-design.md` 소관)
- output 파일 다운로드 정책 자체의 재설계
- 공통 파일 다운로드 인가 개선 (`docs/work-notes/2026/2026-08-18-common-file-download-authorization-remediation.md` 소관)
- 중간파일 실제 다운로드 활성화 (§6 TODO)
- object storage 보존/정리 배치
- nextgen scenario workbench 실행 경로

## 2. 현재 상태 (코드 확인 기준, 2026-09-07 / `feature/gas2`)

### 2.1 파일 연결 구조

| 구분 | `file_rel_obj_spt_cd` | `file_rel_obj_dstg_no` | 적재 주체 |
|---|---|---|---|
| 업로드 입력 파일 | `SCNRCASE` | `tmd_mdl_scnrcasem.case_id` | backend 업로드 API |
| 모델 실행 output 파일 | `SCNRCASEH` | `tmd_mdl_scnrcaseh.run_seq` | model pod 직접 INSERT |
| **모델 실행 중간파일 (신규)** | **`SCNRCASEHI`** (2026-09-07 확정) | `tmd_mdl_scnrcaseh.run_seq` | model pod 직접 INSERT |

```text
tmd_mdl_scnrcasem (case_id)
  └─ tcm_filel  (SCNRCASE  + case_id)     : input  3개 (DEMAND/SUPPLY/TRADE)
  └─ tmd_mdl_scnrcaseh (run_id PK, run_seq UNIQUE)
       ├─ tcm_filel (SCNRCASEH + run_seq) : output 파일
       └─ tcm_filel (SCNRCASEHI + run_seq) : 중간파일 n개   ← 이번 변경
```

### 2.2 확인된 구현 사실

- 상세 조회 조립: `FundamentalModelCaseServiceImpl.getCaseDetailInternal` (`src/main/java/com/sk/domain/mi/service/fundamentalmodel/FundamentalModelCaseServiceImpl.java:228`)
  - input: `findDownloadFiles(caseId, "SCNRCASE")` (같은 파일 `:1746`)
  - output: `findOutputFiles(latestRunHistory)` → `findDownloadFiles(runSeq, "SCNRCASEH")` (같은 파일 `:1821`)
  - 조회 repository: `UploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc` (`del_yn = 'N'` + 유효종료일자 필터 + `file_no ASC`)
- 응답 DTO: `FundamentalModelCaseFilesRes(input, output)`, 각 원소는 `FundamentalModelDownloadFileRes(fileNm)` **단일 필드**
- 계약 고정 테스트 존재: `src/test/java/com/sk/domain/mi/service/fundamentalmodel/FundamentalModelDownloadFileResContractTest.java`
  - `FundamentalModelDownloadFileRes` 의 OpenAPI 속성이 `fileNm` **하나뿐**임을 단정한다. 필드를 추가하면 이 테스트는 실패한다.
- 상세 조회 controller 설명에 "다운로드 기능이 제공되기 전까지 `fileNo` 와 `fileSize` 는 포함하지 않는다"가 명시돼 있다 (`FundamentalModelCaseController.java:83`)
  - 이는 input/output 다운로드를 아직 제공하지 않는 동안 파일 식별자 노출을 줄이기 위한 **의도된 결정**이다. 중간파일도 동일 규칙을 따른다
- 공통 단건 다운로드는 `FileDownloadReq(fileNo, fileNm, fileSize)` 3개를 모두 요구하고, 3개가 DB 값과 일치해야 통과한다 (`FileService.downloadFile`, `FileService.java:423`)
- `run_seq` 는 `INT UNSIGNED` 이고 `file_rel_obj_dstg_no` 와 타입 호환된다

### 2.3 결론

중간파일은 output 과 `file_rel_obj_dstg_no`(= `run_seq`) 가 동일하므로 **구분 축이 하나 더 필요**하다. 이것이 이번 설계의 핵심 결정 항목이다.

다운로드에 필요한 식별자(`fileNo`, `fileSize`)는 현재 의도적으로 노출하지 않으며, 이 결정은 이번 변경에서 유지한다.
따라서 이번 슬라이스의 산출물은 **중간파일 파일명 목록 노출**까지다.

## 3. 요구사항

- R1. 모델이 실행 중 생성하는 중간파일을 object storage 에 올리고 `tcm_filel` 에 기록한다. (모델 측 책임)
- R2. 중간파일은 `run_seq` 로 실행 단위에 귀속되지만 output 파일과 명확히 구분된다.
- R3. `GET /api/v1/mi/fundamental-model/cases/{caseId}` 상세 조회 응답에 중간파일 목록이 포함된다.
- R4. (TODO, 이번 범위 밖) 사용자가 그 목록으로 중간파일을 다운로드할 수 있다. 현재는 파일명만 제공한다.
- R5. 중간파일 개수는 고정 3개가 아니라 n개다. 개수 검증을 두지 않는다.

## 4. 결정 1 — 중간파일 식별 방식

### 후보 비교

| 안 | 방식 | 장점 | 비용/위험 |
|---|---|---|---|
| **A. 신규 관계코드** (권장) | `file_rel_obj_spt_cd = '<신규코드>'`, `file_rel_obj_dstg_no = run_seq` | 기존 조회 메서드/인덱스/패턴 그대로 재사용. 테이블 변경 0. output 과 완전 분리. pod 는 INSERT 의 코드 값만 다르게 넣으면 됨 | `tcm_commcdd` 코드 1건 등록(DBA). 모델팀과 코드 문자열 합의 필요. 다운로드를 나중에 활성화할 때 인가 registry 등록이 필요하다(§10.3) |
| B. 파일명/경로 규칙 | `SCNRCASEH` 유지 + 파일명 prefix 나 storage path 로 구분 | DB/공통코드 변경 0 | 문자열 규칙에 의존해 조용히 깨진다. 모델팀이 파일명을 바꾸면 중간파일이 output 목록에 섞인다. 검증·인가 근거로 쓸 수 없음 |
| C. 매핑 테이블 신규 | `tmd_mdl_scnrcasefilel(run_seq, file_no, file_spt_cd, sort_seq, ...)` | 파일 종류·순서·표시명 등 확장 메타 보유. 종류가 늘어도 코드가 늘지 않음 | 신규 테이블 DDL + rollout. pod 가 2개 테이블에 INSERT(정합성 책임 증가). backend join 조회 추가. 현재 요구(파일명 목록 노출) 대비 과설계 |
| D. `tcm_filel` 컬럼 추가 | 예: `file_rel_obj_sub_cd` 추가 | 코드 값 증가 없이 하위 구분 | 전 도메인 공용 테이블 변경이라 영향 범위가 가장 크다. 기존 엔티티/조회 전반 회귀 위험. DBA 합의 난도 높음 |

### 결정 (2026-09-07 확정)

**A안 채택**. 사용자 결정으로 신규 `file_rel_obj_spt_cd` 코드를 추가한다. B/C/D 안은 채택하지 않는다.
현재 요구는 "run 단위 파일을 종류별로 목록 조회한다" 하나이고, 기존 input/output 이 이미 같은 메커니즘으로 동작한다.
파일 종류가 3종 이상으로 늘거나 파일별 표시 메타(설명, 정렬, 단계명)가 필요해지면 그 시점에 C안을 재검토한다.

### 코드 값 (2026-09-07 확정)

**`SCNRCASEHI`** 로 확정한다. `SCNRCASEH`(실행 이력) + Intermediate 이며, `file_rel_obj_dstg_no` 가 `run_seq` 임이 코드값에서 드러난다.

검토했으나 채택하지 않은 후보:

| 후보 | 성격 | 불채택 사유 |
|---|---|---|
| `SCNRCASEI` | 부모 기준, 짧게 | `SCNRCASE` 와 형태가 비슷해 `dstg_no` 를 `case_id` 로 오해할 여지 |
| `SCNRCASEM` | 중간(Middle) | 마스터 테이블 `tmd_mdl_scnrcasem` 과 혼동 |
| `INTRFILE` | **파일 종류 중심** (부모 없이 "중간 파일" 자체를 약어로) | 아래 별도 설명 |
| `SCNRCASEHINTR` | 부모 + 역할 (`CPNYLOGO` 패턴) | 가독성은 가장 좋지만, 용어사전에 `중간` 표준단어 등록 절차가 추가로 필요해 이번에는 채택하지 않음 |

### `INTRFILE` 류(파일 종류 중심 명명)를 쓰지 않는 이유

읽기 쉽다는 장점은 분명하지만, 이 코드 그룹의 의미와 어긋난다.

- 컬럼이 `file_rel_obj_spt_cd`(파일**관계객체**구분코드)이고 짝이 `file_rel_obj_dstg_no`(파일**관계객체**식별번호)다.
  기존 8개 값(`TEMP`, `BBS`, `GDATS`, `GUDAT`, `NWSTOP`, `CPNYLOGO`, `SCNRCASE`, `SCNRCASEH`)은 **모두 부모 객체**를 가리킨다.
- 파일 종류만 담으면 `dstg_no` 에 `case_id` 를 넣을지 `run_seq` 를 넣을지 코드값에서 알 수 없다.
  이 설계에서 가장 위험한 실패 모드가 "잘못된 값으로 INSERT 해도 오류 없이 목록만 비는 것"이라 특히 불리하다.
- `file_rel_obj_spt_cd` 는 플랫폼 전역 그룹이다. 일반명을 선점하면 다른 도메인(시나리오 워크벤치, 뉴스레터 등)이
  중간파일을 갖게 될 때 `dstg_no` 의미가 달라 재사용할 수 없다.
- 용어사전(`docs/conventions/db-term-dictionary-sheet2.csv`) 확인 결과 `파일 = FILE`(id 91)은 등록돼 있으나
  `중간`(`INTR`/`MID`/`STEP`)은 미등록이다. `STG` 는 이미 **전략(STRATEGY)**(id 73)이 점유 중이라 staging 의미로 쓸 수 없다.
  코드값에 용어사전을 적용한 전례가 있고(2026-06-10 `SCNCASE` → `SCNRCASE`, 사전에 없는 `SCN` 약어), 신규 등록 전례도 있으므로
  (id 365 `이력 = HST`) 등록 자체는 가능하지만 이번 슬라이스에서는 절차 비용을 지지 않기로 했다.

### 용어사전 적용 여부 (`SCNRCASEHI` 기준)

규정 문서상 **공통코드 값은 용어사전 적용 대상이 아니다.** `docs/conventions/db-term-dictionary-usage.md` §3 은
"**테이블명/컬럼명** 약어는 먼저 표준단어명과 표준영문명에서 조회한다"로 적용 범위를 한정하며, `cd_id` 값은 언급하지 않는다.
다만 이 프로젝트에는 코드값에도 사전 기준을 적용한 전례가 있다(2026-06-10 `SCNCASE` → `SCNRCASE`).

`SCNRCASEHI` 의 구성 요소별 근거:

| 요소 | 근거 | 상태 |
|---|---|---|
| `SCNR` | 표준단어 시나리오 | 등록됨 |
| `CASE` | 표준단어 케이스 | 등록됨 |
| `H` | `db-object-naming-rules.md` §3.3 엔터티 유형 코드 `h`(이력). 테이블 `tmd_mdl_scnrcaseh` 및 기존 코드 `SCNRCASEH` 와 동일 | 규칙 근거 있음 |
| `I` | 표준단어도 엔터티 유형 코드도 아닌 신규 접미 식별자 | **근거 규칙 없음** |

즉 `SCNRCASEHI` 도 사전 관점에서 완전히 자유롭지는 않다. `INTR` 은 "단어"로 쓰려는 것이라 등록이 필요한 반면,
`I` 는 단어가 아닌 접미 식별자라 등록 대상 자체가 아니라는 차이일 뿐이다. 규정만 보면 **등록 없이 진행 가능**하다는 판단이지만,
공통코드 담당/DBA 승인 시 이 점을 명시적으로 확인한다(§9 Q7).

trade-off 인정: `SCNRCASEHI` 의 `HI` 접미사는 코드값만으로 뜻이 읽히지 않는다. 이는 `cd_nm`(시나리오 케이스 실행 중간 파일)과
`cd_expn` 으로 설명하며, 향후 파일 종류가 늘어 코드가 더 복잡해지면 §4 C안(매핑 테이블)과 함께 명명을 재검토한다.

| 항목 | 값 |
|---|---|
| `cd_grp_id` | `file_rel_obj_spt_cd` |
| `cd_id` | `SCNRCASEHI` |
| `cd_nm` | 시나리오 케이스 실행 중간 파일 |
| `cd_expn` | 시나리오 케이스 모델 실행 중간 파일 연결 코드 |
| `file_rel_obj_dstg_no` | `tmd_mdl_scnrcaseh.run_seq` |

남은 절차: 공통코드 담당/DBA 등록과 **모델팀의 INSERT 리터럴 반영**.
backend 상수와 pod INSERT 문자열이 다르면 오류 없이 목록만 비어 보이므로 양쪽이 같은 값을 쓰는지 연동 시 확인한다.

## 5. 결정 2 — 상세 조회 노출 형태

### 5.1 노출 위치

| 안 | 내용 | 판단 |
|---|---|---|
| **A. `files.intermediate` 배열 추가** (권장) | `FundamentalModelCaseFilesRes(input, output, intermediate)` | 요청이 "상세 조회에 추가"이고 FE 가 1회 호출로 화면을 구성한다 |
| B. 별도 endpoint | `GET /cases/{caseId}/files/intermediate` | n 이 커져 상세 응답이 비대해지거나 페이징이 필요할 때 재검토 |

### 5.2 대상 run

- output 과 동일하게 **최신 실행(`tmd_mdl_scnrcasem.last_run_id` 가 가리키는 run)의 `run_seq`** 기준으로 조회한다.
- 최신 실행이 없거나 `run_seq` 가 없으면 빈 배열을 반환한다 (현재 `findOutputFiles` 와 동일한 처리).
- 과거 run 의 중간파일 조회는 별도 runs API 계약이 생길 때 다룬다. 이번 범위 밖.

### 5.3 정렬

`file_no ASC` (기존 `findActiveRelationshipFilesOrderByFileNoAsc` 재사용 = pod INSERT 순서).
표시 순서를 모델 실행 단계 순으로 강제해야 한다면 파일명 규칙 또는 C안(매핑 테이블 `sort_seq`)이 필요하다 — 현재는 요구 없음.

## 6. 결정 3 — 다운로드 (이번 범위: 노출하지 않음, TODO)

### 결정 (2026-09-07 확정)

**중간파일도 `fileNm` 만 반환한다.** `fileNo`, `fileSize` 는 노출하지 않으며 이번 슬라이스에서 다운로드 기능을 붙이지 않는다.

근거: 현재 상세 응답이 파일명만 주는 것은 input/output 다운로드를 아직 제공하지 않는 동안 파일 식별자 노출을 줄이려는
**의도된 보안 결정**이다. 중간파일만 예외로 두면 그 결정이 무너진다. 따라서 세 배열(`input`/`output`/`intermediate`)은
동일하게 `fileNm` 단일 필드를 유지한다.

기존 계약 테스트 `FundamentalModelDownloadFileResContractTest` 는 **그대로 통과해야 한다**. 이 테스트가 깨진다면
의도치 않게 식별자를 노출했다는 신호다. `intermediate` 배열에도 같은 단정을 추가한다.

### TODO — 다운로드 활성화 (별도 결정)

다운로드를 붙이기로 하면 클라이언트가 **`fileNo` + `fileNm` + `fileSize`** 를 알아야 한다(§2.2, `FileService.downloadFile`).
그 시점에 아래 3안 중 하나를 선택한다. 이번 범위에서는 채택하지 않는다.

| 안 | 내용 | 장점 | 비용/위험 |
|---|---|---|---|
| 1. `FundamentalModelDownloadFileRes` 에 `fileNo`, `fileSize` 추가 | input/output/intermediate 동일 shape, 공통 다운로드 API 재사용 | 신규 endpoint 0 | 현재의 의도된 비노출 결정을 되돌리는 것이므로 input/output 다운로드 정책과 함께 판단해야 한다. 계약 테스트와 controller 설명 동시 변경 |
| 2. 중간파일 전용 응답 record 신설 | `intermediate` 만 식별자를 갖는다 | input/output 계약을 건드리지 않는다 | 배열마다 필드가 달라 FE/문서가 복잡해진다. 비노출 결정을 중간파일에만 예외 적용하는 셈 |
| 3. 전용 다운로드 endpoint 신설 | `GET /cases/{caseId}/files/{fileNo}/download` 등. 서버가 소속을 검증 후 스트리밍 | 식별자를 클라이언트에 주지 않아도 되고 인가가 가장 강하다. 현재 비노출 정책과 가장 잘 맞는다 | 신규 controller/service/에러계약/OpenAPI. DRM 재암호화·zip 경로 재현 필요 |

현 정책(식별자 비노출)을 유지한 채 다운로드만 제공하려면 3안이 가장 정합적이다. 결정 시 §9 Q2 와 함께 판단한다.

응답 예시 (이번 슬라이스 기준):

```json
{
  "caseId": 101,
  "files": {
    "input":        [{ "fileNm": "DEMAND.xlsx" }],
    "output":       [{ "fileNm": "output.xlsx" }],
    "intermediate": [
      { "fileNm": "step1_demand_curve.csv" },
      { "fileNm": "step2_supply_stack.csv" }
    ]
  }
}
```

## 7. 스키마·공통코드 변경 (delta)

> 물리 스키마와 rollout 문구의 canonical source 는 `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md` 다.
> 아래는 그 문서에 반영할 delta 초안이며, 확정 시 canonical DDL 문서로 옮기고 이 절은 참조만 남긴다.

- 테이블 변경 **없음**. `tcm_filel`, `tmd_mdl_scnrcaseh` 모두 그대로 사용한다.
- 공통코드 detail 1건만 추가한다 (`file_rel_obj_spt_cd` 그룹은 이미 존재).

```sql
-- 모델 실행 중간 파일 코드
INSERT INTO tcm_commcdd (
    cd_grp_id, cd_id, cd_nm, cd_rnk, cd_expn,
    use_bgn_dt, use_end_dt, use_yn, high_cd_grp_id, high_cd_id
)
VALUES (
    'file_rel_obj_spt_cd', 'SCNRCASEHI', '시나리오 케이스 실행 중간 파일', '7',
    '시나리오 케이스 모델 실행 중간 파일 연결 코드',
    '배포실행일자(YYYYMMDD)', '99991231', 'Y', NULL, NULL
);
```

- `cd_rnk = '7'` 은 기존 draft 가 `SCNRCASE = 5`, `SCNRCASEH = 6` 을 쓴 것에 이어붙인 초안이다. 실제 그룹의 `cd_rnk` 중복 여부는 DBA 가 확인한다.

pre-check:

```sql
SELECT cd_id, cd_nm, cd_rnk, use_yn
FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd'
ORDER BY cd_rnk;
```

verify:

```sql
-- 코드 등록 확인
SELECT cd_id, cd_nm, use_yn FROM tcm_commcdd
WHERE cd_grp_id = 'file_rel_obj_spt_cd' AND cd_id = 'SCNRCASEHI';

-- 특정 run 의 중간파일 확인
SELECT file_no, org_file_nm, file_size, del_yn, file_vald_end_dt
FROM tcm_filel
WHERE del_yn = 'N'
  AND file_rel_obj_spt_cd = 'SCNRCASEHI'
  AND file_rel_obj_dstg_no = :run_seq
ORDER BY file_no;
```

rollback:

```sql
-- 참조 row 가 남아 있으면 코드 삭제 금지
SELECT COUNT(*) FROM tcm_filel WHERE file_rel_obj_spt_cd = 'SCNRCASEHI';
-- 0 인 경우에만
DELETE FROM tcm_commcdd WHERE cd_grp_id = 'file_rel_obj_spt_cd' AND cd_id = 'SCNRCASEHI';
```

## 8. 모델(pod) 측 적재 계약

중간파일은 output 과 동일하게 **pod 가 `tcm_filel` 에 직접 INSERT** 한다. backend 는 쓰기에 관여하지 않는다.
아래 항목은 하나라도 어긋나면 상세 조회에서 파일이 **조용히 누락**되므로 모델팀과 문서로 고정해야 한다.

| 컬럼 | 요구값 | 어겼을 때 |
|---|---|---|
| `file_rel_obj_spt_cd` | `'SCNRCASEHI'` (대소문자 포함 정확히 일치) | 목록에 나오지 않거나 output 에 섞임 |
| `file_rel_obj_dstg_no` | Job arg `--run-seq` 값 | 다른 run 에 붙거나 조회 불가 |
| `del_yn` | `'N'` | 조회 제외 |
| `file_vald_end_dt` | `NULL` 또는 `'99991231'` | 과거 일자면 조회 쿼리에서 즉시 제외 |
| `img_conv_cmpt_yn` | `'N'` | NOT NULL 위반으로 INSERT 실패 |
| `org_file_nm` | 화면 표시 파일명, 100자 이내 | 길이 초과 시 INSERT 실패 |
| `strg_path_nm` | `https://{host}/{bucket}/{path}/{file}` 전체 URL, 100자 이내 | 100자 초과 시 INSERT 실패. 형식이 다르면 향후 다운로드 활성화 시 bucket/path 파싱 실패(`SYS_INTERNAL_ERROR`) |
| `file_size` | 실제 byte 수 | 향후 다운로드 활성화 시 크기 검증에서 `FILE_ACCESS_DENIED` |
| `mime_type_nm`, `filex_nm` | 실제 값 | 표시/암호화 대상 판정 품질 저하 |
| `crt_dtm`, `mod_dtm`, `crt_usr_logn_id`, `mod_usr_logn_id` | NOT NULL. 시스템 계정 문자열 사용 | INSERT 실패 |

추가 계약 사항:

- INSERT 시점은 자유(실행 중 순차 업로드 허용). 다만 `RUNNING` 중에도 상세 조회에 부분 목록이 보일 수 있다(§9 Q3).
- 재실행 시 이전 run 의 중간파일은 다른 `run_seq` 에 붙으므로 덮어쓰지 않는다.
- 실패한 run 이 남긴 중간파일도 그대로 남는다. 삭제 책임은 정의하지 않는다(§9 Q5).
- Job args 는 변경 불필요. pod 는 이미 `--run-seq` 를 받는다.

## 9. Open Questions

- ~~Q1. 관계 코드 값 최종 확정~~ → **해소 (2026-09-07)**: `SCNRCASEHI`. 남은 것은 공통코드 등록과 모델팀 리터럴 반영뿐이다.
- Q2. 다운로드 활성화 방식과 인가 범위 (TODO). 2026-09-07 기준 인가 범위 자체는 상세 조회 권한자 전원으로 정리됐으나, 식별자 비노출 정책을 유지하기로 해 **다운로드 기능은 이번 범위에서 제외**한다. 활성화 시 §6 TODO 의 3안 중 선택한다.
- Q3. `RUNNING` 중 부분 업로드된 중간파일을 상세 조회에 노출할지. 현재 가정: **노출**(실패 원인 분석에 유용하고 조회 조건을 추가하지 않는 쪽이 단순하다). 숨기려면 run 상태가 terminal 일 때만 조회하도록 조건을 추가한다.
- Q4. 중간파일 개수/총 용량 상한을 둘지. 초안 권장: 상한 없음(R5). 단 n 이 수십 개 이상이면 §5.1-B(별도 endpoint + 페이징)로 재검토.
- Q5. 보존/정리 정책. output 파일도 동일하게 미정 상태이며(`mi-fundamental-model-output-result-persistence-design.md` open question), 이번 범위 밖으로 둔다.
- ~~Q7. 공통코드 `cd_id` 값 확정~~ → **해소 (2026-09-07)**: `SCNRCASEHI` 로 확정한다. 공통코드 등록은 필요하며, §7 의 `tcm_commcdd` INSERT 문을 DBA/공통코드 담당 rollout 입력으로 사용한다.
- Q6. DRM 정책 (다운로드 활성화 시점 확인): 중간파일 확장자가 암호화 대상(`FileDownloadEncryptor` 정책)에 포함되면 다운로드 시 재암호화되어 내려간다. 이번 범위에서는 다운로드가 없으므로 영향이 없다.

## 10. 영향 범위

### 10.1 코드 변경 목록 (§4-A + §5-A + §6 파일명만 노출)

| 파일 | 변경 |
|---|---|
| `src/main/java/com/sk/domain/mi/service/fundamentalmodel/FundamentalModelCaseServiceImpl.java` | 신규 관계코드 상수 추가, `findIntermediateFiles(latestRunHistory)` 추가(`findOutputFiles` 와 동일 형태), `getCaseDetailInternal` 조립부 수정 |
| `src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/FundamentalModelCaseFilesRes.java` | `intermediate` 필드 추가 |
| `src/main/java/com/sk/domain/mi/controller/fundamentalmodel/FundamentalModelCaseController.java` | 상세 조회 `@Operation` 설명에 중간파일 포함 명시. "fileNo/fileSize 미포함" 문구는 **유지** |
| `src/test/java/com/sk/domain/mi/service/fundamentalmodel/FundamentalModelDownloadFileResContractTest.java` | `intermediate` 배열에도 `fileNm` 단일 필드 단정 추가 (기존 단정은 유지) |

- `FundamentalModelDownloadFileRes` 는 **변경하지 않는다**. `fileNo`/`fileSize` 비노출은 의도된 결정이다.
- 신규 repository 메서드는 필요 없다. `findActiveRelationshipFilesOrderByFileNoAsc` 를 코드 값만 바꿔 재사용한다.
- `FileRelationshipObjectSeparateCode` enum 에는 현재 `SCNRCASE`/`SCNRCASEH` 가 없고 서비스 로컬 상수로 관리된다. 이번 변경도 로컬 상수 패턴을 따르고, enum 승격은 선택 권고로만 남긴다(필수 아님).

### 10.2 문서 변경 목록

| 문서 | 변경 |
|---|---|
| `docs/design-docs/gas/scenario-case-execution-erd-ddl-draft.md` | 공통코드 INSERT/pre-check/verify/rollback 에 신규 코드 추가 (canonical) |
| `docs/design-docs/gas/scenario-case-execution-workflow-design-draft.md` | `tcm_filel` 연결 방식 표에 중간파일 행 추가, Stage 4a 상세 조회 범위 갱신 |
| `docs/feature-specs/mi-fundamental-model-mock-api-feature-spec.md` | `files.intermediate` 계약 추가, "intermediate file metadata 제외" 문구 supersede. `fileNo`/`fileSize` 비노출 제한은 **유지** (canonical API 계약) |
| `docs/design-docs/gas/mi-fundamental-model-agent-model-interface-draft.md` | 모델 책임에 중간파일 업로드/INSERT 계약 추가 |
| `docs/design-docs/gas/mi-fundamental-model-output-result-persistence-design.md` | `SCNRCASEH` 가 run 단위 파일의 유일한 연결이라는 서술 보완 |

### 10.3 보안 영향

- 이번 슬라이스는 파일 **식별자를 새로 노출하지 않는다**. 응답에 추가되는 값은 파일명뿐이다.
- 파일명 자체도 노출 정보다. 모델팀이 중간파일명에 민감 정보(고객사명, 내부 경로 등)를 넣지 않도록 인터페이스 문서에 반영한다.
- 상세 조회 자체의 권한(`SKGAS_COMPANY_USER_ONLY` USER, `THIRD_PARTY_ADMIN_ONLY` ADMIN)은 변경하지 않는다.
- 다운로드를 나중에 활성화할 때 확인할 것 (TODO):
  - 현재 브랜치의 공통 다운로드는 `fileNo` + 파일명 + 크기 일치만 검사한다. 부모 객체 기반 인가는
    `docs/work-notes/2026/2026-08-18-common-file-download-authorization-remediation.md` 에서 별도로 진행 중이며 아직 `In Progress` 다.
  - 그 개선이 머지되면 relation authorizer registry 는 **등록되지 않은 관계 코드를 default deny** 한다.
    `SCNRCASEHI` 를 registry 에 등록하지 않으면 중간파일 다운로드가 전부 `403` 이 된다. 다운로드 활성화 시점에 반드시 함께 처리한다.

## 11. 검증 계획

- focused test: 중간파일 목록 projection(최신 run 있음 / 최신 run 없음 / 파일 0건), `intermediate` 항목이 `fileNm` 만 갖는지 계약 테스트로 단정
  - `./gradlew test -PforceTest --tests 'com.sk.domain.mi.service.fundamentalmodel.*'`
  - 기존 `FundamentalModelDownloadFileResContractTest` 가 **그대로 통과해야 한다**. 실패하면 식별자를 의도치 않게 노출한 것이다.
- 컴파일: `./gradlew compileJava -x test` (동작 검증 아님을 명시)
- generated OpenAPI 에서 `files.intermediate` 존재와 파일 항목이 `fileNm` 단일 속성인지 확인
- DB: §7 verify SQL 로 코드 등록과 run 별 조회 확인
- 모델 연동: 모델팀이 `SCNRCASEHI` 로 INSERT 한 뒤 상세 조회에 파일명이 나오는지 1건 end-to-end 확인

## 12. Fixed Decisions / 확정 대기

### 확정 (Fixed, 2026-09-07)

- D1. 식별 방식: 신규 `file_rel_obj_spt_cd` 코드 + `file_rel_obj_dstg_no = run_seq` (§4-A). 테이블 변경 없이 공통코드 detail 1건만 추가한다.
- D1-1. 코드 값: `SCNRCASEHI` (§4 코드 값)
- D2. 노출 형태: 상세 조회 `files.intermediate` 배열, 최신 run 기준 (§5)
- D3. 다운로드: 이번 범위에서 제공하지 않는다. 파일 항목은 `fileNm` 단일 필드를 유지하고 `fileNo`/`fileSize` 는 계속 비노출한다 (§6). 활성화는 TODO
- D4. 적재 주체: model pod 직접 INSERT, backend 쓰기 없음 (§8)

### 남은 진행 조건

- 공통코드 `SCNRCASEHI` 등록 (DBA/공통코드 담당)
- 모델팀이 동일 리터럴로 INSERT 하도록 인터페이스 문서 반영
- Q3~Q6 은 구현을 막지 않는 운영/정책 항목이다. Q7 은 코드값 확정으로 해소되었다

### TODO (별도 결정 후 진행)

- 중간파일(및 input/output) 다운로드 활성화. §6 TODO 의 3안 중 선택하고, 선택 시 식별자 노출 정책과 공통 다운로드 인가 개선을 함께 판단한다.
