# 인수인계 문서: feature/agent-query-update

> **브랜치**: `feature/agent-query-update`
> **기준 브랜치**: `dev`
> **작업 기간**: 2025-11-06 ~ 2026-01-16
> **작성일**: 2026-03-12

---

## 1. 작업 배경

마리(Mari) 에이전트 외부 API의 **스펙이 변경**되어, 백엔드가 새로운 요청/응답 포맷에 맞게 전면 대응한 작업입니다.

변경 범위:
- 외부 Agent API 요청 DTO 포맷 변경
- SSE(Server-Sent Events) 스트림 응답 구조 변경
- 스트림 연결 관리 및 취소 기능 신규 구현
- 질의 시 `repositoryIds` 파라미터 추가
- `rdb_data` 스트림 이벤트 타입 핸들러 신규 추가

---

## 2. 커밋 흐름 요약 (10개 커밋)

### Phase 1 — 외부 API 스펙 변경 대응 (2025-11-06 ~ 11-17)

| 커밋 | 내용 |
|------|------|
| `0e5dd40f` | mari agent spec 변경 대응 |
| `632544cf` | 마리 에이전트 질의 req, res 포맷 변경 대응 |

**주요 변경 내용**

- `ModelAgentReq` / `ModelAgentRes` / `ModelAgentStreamBody` — 외부 API 요청·응답 DTO 전면 수정
- `StreamEventHandler` 계열 (`Execute`, `Generate`, `Plan`) — 스트림 이벤트 처리 수정
- `QueryRewriterStreamEventHandler` **삭제** (외부 API 스펙에서 제거됨)
- `MariAgentGatewayService`, `MariAgentResParser` — 새 스펙 파싱 로직 변경
- 응답 DTO `AnswerRes`, `StreamRes`, `MessageBundle`, `Series`, `Source` 구조 변경
- `ChatbotTalkQna` 엔티티 저장 필드 구조 변경

---

### Phase 2 — SSE 스트림 관리 신규 구현 (2025-11-25 ~ 12-05)

| 커밋 | 내용 |
|------|------|
| `1a854372` | 컨트롤러 Parameter 어노테이션 단일화 및 미사용 import 제거 |
| `19b8fab0` | Mari Agent stream 응답 변경 대응 및 리팩토링 |
| `2b1d7303` | Mari 응답 저장 포맷 변경 |
| `dc91af64` | MariAgent SSE 스트림 관리 기능 및 취소 로직 추가 |
| `22b3f634` | 질의 Stream 중단 API에 StreamCancelReq DTO 적용 |
| `22b73e61` | 스트림 연결 실패 시 답변 내용 null로 저장 |

**주요 변경 내용**

- **`SseConnectionManager` 신규 추가** — SSE 연결을 `ConcurrentHashMap`으로 관리, 세션별 중단 요청 처리
- **`AgentStreamException` 신규 추가** — 스트림 전용 예외 클래스
- **`Answer.java` 신규 추가** — 응답 저장용 중간 DTO 분리
- **`StreamCancelReq` 신규 추가** — 스트림 중단 API 요청 DTO
- `MiMariAgentController` — 스트림 중단 API 엔드포인트 추가
- `MariAgentService` — 스트림 실패 시 `null` 저장, 예외 케이스 보완
- `AsyncConfig` — 불필요한 비동기 설정 제거

---

### Phase 3 — `repositoryIds` 파라미터 추가 (2025-12-24)

| 커밋 | 내용 |
|------|------|
| `a7eb5120` | 마리 질의 input repositoryIds 추가 |

**주요 변경 내용**

- `GoodsRepository.findGoodsNosAndRepositoryIdsWithPermission()` — 권한 있는 상품의 `gds_no`와 `repository_id`를 함께 조회
- `MariAgentGatewayService` — 조회한 `repositoryIds`를 `ModelAgentReq` 생성 시 전달
- `ModelAgentReq.Input` — `repositoryIds` 파라미터 수신 처리 추가

> ✅ `ModelAgentReq.Input.ReqInput.of()` 빌더에서 `repositoryIds` 정상 세팅 완료. 외부 API 요청 시 `repository_ids` 필드로 전달됨.

---

### Phase 4 — `rdb_data` 스트림 핸들러 추가 (2026-01-16)

| 커밋 | 내용 |
|------|------|
| `b295b9ee` | RDB 데이터 타입 처리 핸들러 추가 |

**주요 변경 내용**

- **`RdbDataStreamEventHandler` 신규 추가** — `rdb_data` 타입 스트림 이벤트 전담 처리
- `ModelAgentStreamBody` — `DataType.RDB_DATA` enum 추가, `isRdbData()` 메서드 추가
- `MariAgentGatewayService` — 핸들러 라우팅 로직에 `rdb_data` 분기 추가 (`vector_data` 다음, `complete` 이전 순서)

**`rdb_data` 이벤트 구조 (외부 API)**

```json
{
  "node": "RdbSearchNode",
  "data": "rdb_data",
  "state": "progress",
  "value": [
    {
      "source": "Hong Leong Investment Bank Bhd",
      "title": "Traders_Brief_0620",
      "gds_no": 70008,
      "gds_data_dstg_id": "116049757"
    }
  ]
}
```

> ⚠️ **미완성**: `RdbDataStreamEventHandler`에서 `value` 내 `gds_no`를 수신하고 있으나 현재 **DB 조회 없이 raw 데이터를 그대로 반환**. `gds_no`로 상품 정보를 조회하여 프론트에 필요한 값(상품 상세 페이지 이동에 필요한 정보 등)을 구해 전달하는 로직이 미구현 상태.

---

## 3. 주요 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `MiMariAgentController` | 스트림 중단 API 추가 |
| `MariAgentService` | 스트림 생명주기 관리, 예외 처리, repositoryIds 연동 |
| `MariAgentGatewayService` | 외부 API 호출 포맷 전면 변경, repositoryIds 전달 |
| `MariAgentResParser` | 응답 파싱: 정형(related_data) + 비정형(vector_metadata) |
| `SseConnectionManager` ⭐ 신규 | SSE 연결 Map 관리 + 스트림 취소 로직 |
| `AgentStreamException` ⭐ 신규 | 스트림 전용 예외 |
| `StreamCancelReq` ⭐ 신규 | 스트림 중단 요청 DTO |
| `ModelAgentReq` | 외부 API 요청 DTO (repositoryIds 포함 예정) |
| `RdbDataStreamEventHandler` ⭐ 신규 | rdb_data 타입 스트림 이벤트 처리 핸들러 |
| `ModelAgentStreamBody` | 스트림 응답 파싱 구조 (RDB_DATA enum 추가) |
| `ModelAgentRes` | 비스트림 응답 파싱 구조 (VectorSource 포함) |
| `ChatbotTalkQna` | 저장 포맷 변경 |

---

## 4. 응답 데이터 처리 구조 (현재 상태)

마리 에이전트 응답은 `MariAgentResParser`에서 **정형 / 비정형** 두 가지로 분류하여 `Related` 리스트로 변환 후 프론트에 전달합니다.

```
외부 Agent API 응답
├── related_data: [gdsDataSourceNo, ...]   → 정형 데이터 (STRUCTURED)
└── vector_metadata[].gds_data_dstg_id    → 비정형 데이터 (UNSTRUCTURED)
```

### 정형 데이터 처리 흐름

```
ModelAgentStreamBody.relatedData()   →   List<Integer> (gdsDataSourceNo)
    ↓
GoodsDataSourceDetailRepository.findByGoodsDataSourceNoInAndIsDeletedFalse()
    ↓
Related.ofStructuredList()   →   { type: STRUCTURED, goodsNo, goodsDataNo, displayNm }
```

### 비정형 데이터 처리 흐름

```
ModelAgentStreamBody.vectorSources()[].gdsDataDstgId   →   List<String>
    ↓
GoodsDataRepository.findByGoodsDataDistinguishIdInAndIsDeletedFalse()
    ↓
UploadedFileRepository.findByFileRelationshipObjectDistinguishNoInAndIsDeletedFalse()
    ↓
Related.ofUnstructuredList()   →   { type: UNSTRUCTURED, goodsNo, goodsDataNo, displayNm }
```

---

## 5. 이어서 해야 할 작업

### 5-1. `rdb_data` 핸들러 내 `gds_no` 조회 및 프론트 전달

`RdbDataStreamEventHandler`에서 외부 API로부터 `gds_no`를 수신하고 있으나, 현재 **DB 조회 없이 raw 데이터를 그대로 프론트에 반환**하는 상태입니다. `gds_no`로 상품 정보를 조회해 상품 상세 페이지 이동에 필요한 값을 구성하여 전달해야 합니다.

**현재 핸들러 처리** (`RdbDataStreamEventHandler.java`)

```java
// rdb_data value 예시
// [{ "source": "...", "title": "...", "gds_no": 70008, "gds_data_dstg_id": "116049757" }]

Object value = streamBody.value();
List<Map<String, Object>> dataList = (List<Map<String, Object>>) value;

// ⚠️ 현재: gds_no 조회 없이 raw value 그대로 반환
StreamRes streamRes = StreamRes.builder()
    .qnaNo(qna.getChatbotTalkQnaNo())
    .data(value)
    .build();
```

**작업 내용**:
1. `dataList`에서 `gds_no` 추출
2. `gds_no`로 `Goods` (또는 `GoodsData`) 조회 — 상품 상세 페이지 이동에 필요한 정보 구성
3. 조회 결과를 `StreamRes` 또는 별도 DTO에 담아 프론트에 전달

**예상 수정 파일**:
- `RdbDataStreamEventHandler` — `gds_no` 기반 DB 조회 로직 추가
- `MariAgentResParser` — `prepareRelatedData()` 활용 또는 신규 메서드 추가
- `StreamRes` 또는 신규 DTO — `gds_no` 조회 결과 필드 추가 (필요 시)

---

## 6. 참고 — 관련 파일 경로

```
src/main/java/com/sk/
├── domain/mi/
│   ├── controller/MiMariAgentController.java
│   ├── dto/
│   │   ├── request/agent/StreamCancelReq.java          ⭐ 신규
│   │   └── response/agent/
│   │       ├── StreamRes.java                          ⭐ 신규
│   │       ├── StreamResponseWrapper.java              ⭐ 신규
│   │       └── talk/
│   │           ├── Answer.java                         ⭐ 신규
│   │           ├── AnswerRes.java
│   │           └── Related.java
│   ├── exception/AgentStreamException.java             ⭐ 신규
│   └── service/agent/
│       ├── MariAgentService.java
│       ├── MariAgentGatewayService.java
│       ├── MariAgentResParser.java
│       └── stream/
│           ├── SseConnectionManager.java               ⭐ 신규
│           ├── RdbDataStreamEventHandler.java          ⭐ 신규
│           ├── ExecuteStreamEventHandler.java
│           ├── GenerateStreamEventHandler.java
│           ├── PlanStreamEventHandler.java
│           └── StreamEventHandler.java
└── domain/shared/client/dto/
    ├── request/ModelAgentReq.java
    └── response/
        ├── ModelAgentRes.java
        └── ModelAgentStreamBody.java
```
