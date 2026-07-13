## Document Control

- status: Draft
- canonical for: Reference only
- owner: Shared
- source documents:
  - `docs/feature-specs/mi-model-parameter-proposal-backend-feature-spec.md`
  - `docs/feature-specs/mi-model-param-proposal-current-state-frontend-guide.md`
  - FE review feedback: 기존 A4a 응답을 유지하면서 `modelFiles` 기준 tree 구조로 제공
  - `docs/work-notes/2026/2026-07-10-model-param-proposals-response-shape-review.md`
- depends on:
  - `docs/conventions/source-of-truth-governance.md`
- supersedes:
  - 이 문서의 이전 `parameterSet.items + navigation` 제안
- last synced: 2026-07-10
- open questions:
  - leaf collection 이름을 `parameters`로 확정할지
  - FE가 현재 top-level `parameters`, `modelFiles`, `parameterShapes`를 이미 연동했는지와 호환 기간이 필요한지
  - `execPrepId`, `scenarioGroupId`, `eventId`를 FE 응답에 유지할 필요가 있는지
  - 응답 시각을 offset 포함 형식으로 확정할지
  - 실패 응답을 기존 `failureReason`으로 유지할지 typed failure object로 별도 개선할지

# 수급모델 파라미터 현재 상태 응답 개선안 — FE 검토 초안

> 이 문서는 FE 의견을 반영한 **제안 초안**입니다.
> 현재 운영·개발 응답 계약이 아니며, FE/BE 합의 후 canonical feature spec에 반영되어야 구현 기준이 됩니다.

## 1. FE 피드백 반영 방향

기존 응답은 아래 세 배열이 분리되어 있습니다.

- `parameters[]`: Agent의 실제 파라미터 데이터
- `modelFiles[]`: 파일·시트·테이블 탐색용 tree
- `parameterShapes[]`: 파라미터별 표 렌더링 정보

이 구조에서는 FE가 `parameterName`과 model target 좌표로 데이터를 다시 결합해야 합니다.

FE 피드백에 따라 다음 방향으로 수정 제안합니다.

- 기존 top-level 상태·식별·실행 필드는 유지
- `modelFiles`를 `modelFile > modelSheet > modelTable > parameter`의 단일 tree로 확장
- 각 parameter leaf에 Agent 원본 `parameter`와 화면용 `rendering`을 함께 배치
- 중복 projection인 top-level `parameters`, `parameterShapes`는 clean target contract에서 제거
- A6 실행 요청은 tree의 `parameter`만 flat array로 추출하며 기존 request contract 유지

## 2. 대상 API

```http
GET /api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals
```

- 용도: 팝업 진입 또는 SSE 수신 후 Scenario 기준 최신 파라미터 제안·확정·실행 상태 조회
- request body: 없음
- query parameter: 없음
- 공통 `BaseResponse<T>` wrapper 유지
- schema/ERD 및 조회 기준 변경 없음

## 3. 제안 응답 예시

아래 예시는 Agent가 제안 생성을 완료해 사용자 검토를 기다리는 `PROPOSAL_READY` 상태입니다.

```json
{
  "status": true,
  "code": "SUC-000",
  "message": "Success.",
  "data": {
    "scenarioId": 309,
    "currentState": "PROPOSAL_READY",
    "parameterSource": "PROPOSAL_PARAMETERS",
    "execPrepId": 51001,
    "modelParamProposalId": 10001,
    "scenarioGroupId": 61,
    "eventId": 1378,
    "caseId": null,
    "runId": null,
    "modelExecutionStatus": null,
    "modelFiles": [
      {
        "modelFile": "DEMAND",
        "modelSheets": [
          {
            "modelSheet": "WeatherAdjustment",
            "modelTables": [
              {
                "tableName": "Table2",
                "parameters": [
                  {
                    "parameter": {
                      "parameterName": "Temperature",
                      "modelTarget": {
                        "modelFile": "DEMAND",
                        "modelSheet": "WeatherAdjustment",
                        "tableName": "Table2",
                        "valueFieldNames": [
                          "Temperature"
                        ],
                        "slotAxesFieldNames": [
                          "Country",
                          "Year",
                          "Month"
                        ]
                      },
                      "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
                      "values": [
                        {
                          "Country": "Germany",
                          "Year": 2027,
                          "Month": 1,
                          "basecaseValue": 1.2,
                          "proposedValue": -3.8,
                          "rationale": "Germany 2027년 1월 한파 영향을 반영했습니다."
                        }
                      ]
                    },
                    "rendering": {
                      "readMode": "FIXED_COLUMNS",
                      "columns": [
                        {
                          "column": "B",
                          "role": "Country",
                          "kind": "SLOT",
                          "align": "LEFT"
                        },
                        {
                          "column": "C",
                          "role": "Year",
                          "kind": "SLOT",
                          "align": "RIGHT"
                        },
                        {
                          "column": "D",
                          "role": "Month",
                          "kind": "SLOT",
                          "align": "RIGHT"
                        },
                        {
                          "column": "F",
                          "role": "Temperature",
                          "kind": "VALUE",
                          "align": "RIGHT"
                        }
                      ],
                      "semanticAxes": "Country, Year, Month 기준 기온 조정값"
                    }
                  }
                ]
              }
            ]
          }
        ]
      }
    ],
    "failureReason": null,
    "requestedAt": "2026-07-03T15:00:00+09:00",
    "completedAt": "2026-07-03T15:00:20+09:00",
    "confirmedAt": null
  }
}
```

## 4. Tree 계약

### 4.1 계층

```text
modelFiles[]
└── modelSheets[]
    └── modelTables[]
        └── parameters[]
            ├── parameter   // Agent 원본, A6 전송용
            └── rendering   // 화면 렌더링 정보
```

| 경로 | 역할 |
| --- | --- |
| `modelFiles[].modelFile` | 모델 파일 그룹 |
| `modelSheets[].modelSheet` | 모델 시트 그룹 |
| `modelTables[].tableName` | 모델 테이블 그룹 |
| `modelTables[].parameters[]` | 해당 파일·시트·테이블에 속한 parameter leaf 목록 |
| `parameters[].parameter` | Agent parameter 원본 shape |
| `parameters[].rendering` | 해당 parameter를 표로 표시하기 위한 메타데이터 |

기존 `modelSheets[].tableNames[]`는 이름만 가진 배열이므로 실제 parameter를 담을 수 없습니다. 개선안에서는 이를 `modelTables[]` node로 변경합니다.

### 4.2 `parameter`

`parameter`는 Agent flat parameter 원소와 동일한 JSON shape를 유지합니다.

- `parameterName`
- `modelTarget`
- parameter-level `rationale`
- `values[]`

`modelTarget.modelFile`, `modelTarget.modelSheet`, `modelTarget.tableName`을 tree 경로와 중복해서 유지하는 이유는 A6 `confirmationParameters[]`에 원본 parameter를 그대로 투영하기 위해서입니다.

### 4.3 `rendering`

`rendering`은 현재 `parameterShapes[]`의 화면 렌더링 정보만 leaf에 결합한 객체입니다.

- `readMode`
- `columns[]`
- `semanticAxes`

다음 좌표 필드는 반복하지 않습니다.

- `parameterName`
- `modelFile`
- `modelSheet`
- `modelTable` 또는 `tableName`

parameter identity와 위치는 `parameter.parameterName`, tree 경로, `parameter.modelTarget`을 사용합니다.

### 4.4 순서와 identity

- `modelFiles`, `modelSheets`, `modelTables`는 선택된 Agent parameter 배열에서 각 값이 최초 등장한 순서를 유지
- 같은 table 안의 `parameters`도 Agent parameter 최초 등장 순서 유지
- parameter identity는 `parameterName` 단독이 아니라 다음 조합으로 판단
  - `parameterName`
  - `modelTarget.modelFile`
  - `modelTarget.modelSheet`
  - `modelTarget.tableName`
  - `modelTarget.valueFieldNames`
  - `modelTarget.slotAxesFieldNames`
- FE는 배열 index나 `parameterName` 단독을 전역 key로 사용하지 않음
- 빈 file/sheet/table node는 반환하지 않음

## 5. 응답 정합성 규칙

- 반환 대상 parameter는 tree에 정확히 한 번만 포함
- 모든 parameter leaf는 `parameter`와 `rendering`을 각각 하나씩 가져야 함
- tree 경로는 아래 값과 완전히 일치해야 함
  - `modelFiles[].modelFile = parameter.modelTarget.modelFile`
  - `modelSheets[].modelSheet = parameter.modelTarget.modelSheet`
  - `modelTables[].tableName = parameter.modelTarget.tableName`
- Agent payload와 rendering master의 파일·시트·테이블 좌표가 다르면 한쪽 값을 임의로 선택하지 않고 데이터 정합성 오류 처리
- rendering master가 없거나 중복이면 불완전 leaf를 반환하지 않고 데이터 정합성 오류 처리
- `parameterSource=NONE`이면 `modelFiles=[]`
- `parameterSource`가 `PROPOSAL_PARAMETERS` 또는 `CONFIRMATION_PARAMETERS`인데 실제 parameter payload가 없으면 정상 empty tree로 숨기지 않고 데이터 불일치로 처리
- tree 경로 값 비교는 case-sensitive exact match

## 6. 상태별 반환 규칙

기존 `currentState`, `parameterSource`, 실행 필드의 의미는 유지합니다.

| `currentState` | `parameterSource` | `modelFiles` | Case/Run 필드 | `failureReason` |
| --- | --- | --- | --- | --- |
| `NOT_REQUESTED` | `NONE` | `[]` | `null` | `null` |
| `GENERATING` | `NONE` | `[]` | `null` | `null` |
| `PROPOSAL_READY` | `PROPOSAL_PARAMETERS` | 제안값 tree | `null` | `null` |
| `CONFIRMED_PENDING_EXECUTION` | `CONFIRMATION_PARAMETERS` | 확정값 tree | `null` | `null` |
| `EXECUTING` | `CONFIRMATION_PARAMETERS` | 확정값 tree | `RUNNING` | `null` |
| `EXECUTED_CONFIRMED` | `CONFIRMATION_PARAMETERS` | 확정값 tree | `COMPLETED` | `null` |
| `PROPOSAL_FAILED` | `NONE` | `[]` | `null` | 제안 실패 정보 |
| `EXECUTION_FAILED` | 확정값이 있으면 `CONFIRMATION_PARAMETERS`, 없으면 `NONE` | 확정값이 있으면 tree, 없으면 `[]` | 상황에 따라 `FAILED` 또는 `null` | 실행 실패 정보 |

## 7. A6 실행 요청과의 연결

A4a 응답 개선으로 A6 request JSON 구조는 변경하지 않습니다.

```http
POST /api/v1/mi/next-gen/scenarios/{scenarioId}/model-executions
Content-Type: application/json
```

FE mapping:

| A4a response | A6 request |
| --- | --- |
| `data.modelParamProposalId` | `modelParamProposalId` |
| 모든 `data.modelFiles[].modelSheets[].modelTables[].parameters[].parameter`를 flatten | `confirmationParameters[]` |
| `parameters[].rendering` | 전송하지 않음 |
| file/sheet/table tree wrapper | 전송하지 않음 |
| 상태·실행·실패 필드 | 전송하지 않음 |

사용자가 `proposedValue`를 수정한 뒤 보내는 A6 request 예시:

```json
{
  "modelParamProposalId": 10001,
  "confirmationParameters": [
    {
      "parameterName": "Temperature",
      "modelTarget": {
        "modelFile": "DEMAND",
        "modelSheet": "WeatherAdjustment",
        "tableName": "Table2",
        "valueFieldNames": [
          "Temperature"
        ],
        "slotAxesFieldNames": [
          "Country",
          "Year",
          "Month"
        ]
      },
      "rationale": "한파 강도가 기온 하락으로 직접 표현되므로 Temperature 조정 대상입니다.",
      "values": [
        {
          "Country": "Germany",
          "Year": 2027,
          "Month": 1,
          "basecaseValue": 1.2,
          "proposedValue": -3.5,
          "rationale": "Germany 2027년 1월 한파 영향을 반영했습니다."
        }
      ]
    }
  ]
}
```

A6 전송 규칙:

- FE가 변경할 수 있는 실행값은 각 row의 `proposedValue`
- `parameterName`, `modelTarget`, slot-axis 값, `basecaseValue`는 A4a에서 받은 값을 유지
- `basecaseValue=null`이면 key를 제거하지 않고 `null` 그대로 전송
- parameter 및 value `rationale`은 사용자 편집 대상은 아니지만 required transport field이므로 유지
- `rendering`, tree node, checkbox 상태, row index, validation message 같은 UI 전용 필드는 전송하지 않음

## 8. DTO 구조 초안

아래는 JSON 책임을 설명하기 위한 개념 구조이며 Java class 이름은 구현 시 확정합니다.

```text
CurrentModelParamProposalRes
├── existing state / identifier / execution fields
└── List<ModelFileTreeNode> modelFiles
    ├── String modelFile
    └── List<ModelSheetTreeNode> modelSheets
        ├── String modelSheet
        └── List<ModelTableTreeNode> modelTables
            ├── String tableName
            └── List<ModelParameterTreeLeaf> parameters
                ├── ModelParameterRes parameter
                └── ModelParameterRenderingRes rendering
```

public response DTO와 Agent client request DTO는 Java class를 직접 공유하지 않습니다. JSON field shape만 동일하게 유지하고 service 경계에서 변환합니다.

현재 `ModelFileNode`는 A4a와 특정 proposal 상세 GET의 `ModelParamProposalRes`가 함께 사용합니다. A4a만 변경하는 이번 slice에서는 기존 shared DTO의 내부 shape를 바로 바꾸지 않고 A4a 전용 tree DTO를 도입해야 특정 proposal 상세 응답이 의도치 않게 변경되지 않습니다.

## 9. 대표 오류 시나리오

| 상황 | HTTP | 공통 오류 코드 예시 | 비고 |
| --- | --- | --- | --- |
| Scenario 없음 | `404` | `RSC-001` | 기존 공통 오류 계약 유지 |
| `FILE_UPLOAD` 모드 Scenario | `409` | `BIZ-001` | 파라미터 제안 대상이 아님 |
| Agent payload와 rendering master 좌표 불일치 | `409` | `BIZ-001` | 불완전 tree 대신 정합성 오류 |
| parameter에 대응하는 rendering master 없음/중복 | `409` | `BIZ-001` | leaf 누락 또는 임의 선택 금지 |
| Agent parameter JSON 파싱 실패 | `502` | 기존 API response error | 저장 payload 불량 |
| 내부 처리 오류 | `500` | `SYS-001` | 내부 예외 문자열 미노출 |

## 10. 호환 및 rollout

### FE가 기존 세 배열을 아직 사용하지 않는 경우

- canonical feature spec을 먼저 갱신
- top-level `parameters`, 기존 navigation-only `modelFiles`, `parameterShapes`를 새 tree `modelFiles`로 교체
- focused serialization/service test 후 FE와 sample JSON 재확인

### FE가 기존 세 배열을 이미 사용 중인 경우

- 같은 필드명 `modelFiles`의 내부 shape 변경도 breaking change로 취급
- 아래 중 하나를 FE/BE가 합의
  - 신규 version endpoint/response 도입
  - 신규 tree field를 additive 제공한 뒤 기존 필드 deprecate
- migration 완료 전 기존 필드를 즉시 제거하지 않음

## 11. FE 최종 확인 요청

1. tree leaf collection 이름을 `parameters`로 사용해도 될까요?
2. 각 leaf를 `{ parameter, rendering }` wrapper로 제공하면 화면 구현과 A6 mapping에 적합한가요?
3. 기존 top-level `parameters`, `modelFiles`, `parameterShapes`를 이미 연동했나요? 연동했다면 호환 기간이 필요합니다.
4. `modelFiles > modelSheets > modelTables > parameters` 순서가 팝업의 탭·테이블 구조와 일치하나요?
5. FE에서 `execPrepId`, `scenarioGroupId`, `eventId`를 실제로 사용하나요?
6. 시각 값에 `+09:00`과 같은 offset이 필요하나요?
7. 실패 화면은 기존 `failureReason` 문자열로 충분한가요, 아니면 `stage/code/message`가 필요한가요?

## 12. API 및 데이터 영향

- schema/ERD 변경: 없음
- 조회 기준 및 상태 전이 변경: 없음
- Agent 저장 payload 변경: 없음
- A6 request wire contract 변경: 없음
- data access: 현행 JPA 조회 및 parameter master 일괄 조회 유지
- 예상 구현 변경:
  - A4a 전용 response tree DTO (`ModelParamProposalRes`의 상세 GET DTO와 분리)
  - `modelFiles` tree assembly
  - payload/master completeness 및 좌표 정합성 검증
  - OpenAPI/feature spec/frontend guide
  - 상태별 serialization 및 service focused test

## 13. 범위 경계

이번 검토에 포함:

- A4a current-state response의 `modelFiles` 중심 tree
- tree node/leaf/null/정합성 규칙
- A4a tree에서 A6 flat `confirmationParameters[]`로의 FE mapping

이번 검토에서 제외:

- proposal 생성 POST 계약
- 특정 proposal 상세 GET 계약 변경 여부
- SSE event 계약
- A6 실행 처리·응답 계약 변경
- Agent payload 저장 형식
- schema/DDL 변경

## 14. 구현 준비 상태

- 현재 상태: `blocked by open questions`
- tree 방향 자체는 FE 피드백으로 결정됨
- 구현 전 최소 확정 필요:
  - leaf collection 및 wrapper field 이름
  - 기존 FE 연동 여부와 호환 방식
- 합의 후 API path, response fields, nullability, 예시는 canonical feature spec으로 이동
