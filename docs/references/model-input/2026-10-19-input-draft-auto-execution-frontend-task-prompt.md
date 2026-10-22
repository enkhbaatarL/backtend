# Input Draft 자동 실행 FE 작업 프롬프트

## Document Control

- status: Complete
- canonical for: Reference only — FE 작업 인계 프롬프트
- owner: FE / BE
- last synced: 2026-10-19
- source documents:
  - `docs/feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md` §9.1~9.5
  - `docs/feature-specs/mi-model-input-current-state-frontend-guide.md` §6
  - `src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/ModelInputDraftResponses.java`
- supersedes: 없음
- open questions: 없음

아래 블록을 frontend 저장소의 작업 프롬프트로 사용한다. API 정본은 backend의 위 명세다. 이 문서는 인계 시점의 필요한 계약 요약이며 새 API 정본이 아니다.

```text
frontend 저장소의 AGENTS.md를 따르라.

목표:
Input Draft 화면의 최종저장 동작을 BE 자동 수급모델 실행 흐름에 맞춘다. 사용자의 한 번의 동작으로 저장 접수부터 모델 실행 결과까지 표시하고, 저장 실패와 저장 성공 후 실행 실패를 구분한다.

관련 코드·정본:
- backend/docs/feature-specs/mi-fundamental-model-input-draft-editing-api-proposal.md §9.1~9.5, 특히 §9.2.1.
- backend/docs/feature-specs/mi-model-input-current-state-frontend-guide.md §6.
- backend/src/main/java/com/sk/domain/mi/dto/response/fundamentalmodel/ModelInputDraftResponses.java의 FinalizationView/FinalizationExecution 및 생성 OpenAPI.
- FE 탐색 시작점: src/service/mi/fundamentalModel/case.ts, src/pages/mi/fundamentalModel/components/FundamentalModelRunList.tsx 및 현재 Input Draft의 최종저장 호출·응답 타입·폴링 코드. 실제 사용 경로를 먼저 확인하고 가까운 기존 방식을 따른다.

API 변경·흐름:
- 공통 prefix: /api/v1/mi/fundamental-model.
- POST /input-drafts/{draftId}/finalizations의 body는 기존 {requestId, baseRevision, title, description?} 그대로다. 저장되지 않은 변경은 기존 임시저장을 먼저 완료하고 서버의 성공 revision을 사용한다.
- POST는 저장 진행이면 202, 종료된 작업/완료 재전송이면 200이다. Location과 data.statusEndpoint는 같은 최종저장 GET 경로다. GET /input-drafts/{draftId}/finalizations/{operationId}는 작업 실패도 HTTP200으로 조회한다.
- BaseResponse.status=true 및 HTTP200/202는 모델 계산 성공을 뜻하지 않는다. data.status는 저장 상태(QUEUED/RUNNING/SUCCEEDED/FAILED)다.
- BE가 저장된 Case의 Run을 자동 접수한다. 이 흐름의 FE가 별도로 POST /cases/{caseId}/runs를 자동 호출하는 코드는 제거한다. 기존 파일 업로드 화면의 명시적 실행 기능은 유지한다.
- FinalizationView에 필수 nullable 필드 execution이 추가됐다. 저장 전/저장 실패와 배포 전 이미 완료한 저장은 null이다. 나머지 저장 결과 필드는 유지된다.
- execution 타입:
  {status: 'QUEUED'|'RUNNING'|'COMPLETED'|'FAILED', statusLabel: string,
   runId: string|null, statusEndpoint: string, failureReason: string|null}
- runId는 최초 연결한 모델 실행 ID이며, 실행 접수 전/접수 실패에는 null이다. execution.statusEndpoint는 /cases/{caseId}/status이며 Case의 최신 Run을 조회한다. data.statusEndpoint는 최종저장 작업 조회 경로이므로 둘을 혼동하지 않는다.
- failureReason은 FAILED에서만 EXECUTION_REQUEST_FAILED(실행 접수 실패), JOB_SUBMIT_FAILED(Job 요청 실패), MODEL_EXECUTION_FAILED(그 외 모델 실패) 중 하나다. 다른 상태에서는 null이다. 표시에는 statusLabel을 활용하고 failureReason별 안전한 사용자 문구를 제공한다.

저장 성공 후 실행 실패 응답의 data 예시:
{
  "operationId":"aefb60dc-18b8-4ac2-beb4-a4d499950766",
  "requestId":"aefb60dc-18b8-4ac2-beb4-a4d499950767",
  "draftId":"aefb60dc-18b8-4ac2-beb4-a4d499950765",
  "status":"SUCCEEDED","statusLabel":"최종저장 완료","baseRevision":1,
  "finalizedCaseId":104,
  "files":[{"role":"SUPPLY","fileNo":501},{"role":"DEMAND","fileNo":502},
           {"role":"TRADE","fileNo":503},{"role":"OUTPUT","fileNo":504}],
  "caseEndpoint":"/api/v1/mi/fundamental-model/cases/104",
  "failure":null,
  "statusEndpoint":"/api/v1/mi/fundamental-model/input-drafts/aefb60dc-18b8-4ac2-beb4-a4d499950765/finalizations/aefb60dc-18b8-4ac2-beb4-a4d499950766",
  "execution":{"status":"FAILED","statusLabel":"실행 실패","runId":null,
    "statusEndpoint":"/api/v1/mi/fundamental-model/cases/104/status",
    "failureReason":"EXECUTION_REQUEST_FAILED"}
}

상태별 화면·폴링 규칙:
1. data.status=QUEUED/RUNNING: 저장 대기/처리 중 표시. data.statusEndpoint를 2초 간격으로 조회한다. 이때 execution=null이어도 저장 진행 중이므로 폴링을 종료하지 않는다.
2. data.status=FAILED: 저장 실패를 표시하고 폴링을 종료한다. failure를 사용하며 기존 성공 Draft/편집 상태를 유지한다. 새 Case나 모델 결과로 이동하지 않는다.
3. data.status=SUCCEEDED + execution.QUEUED: 저장 완료·실행 대기 표시. 같은 data.statusEndpoint를 계속 2초 간격으로 조회한다. 저장 SUCCEEDED만 보고 모델 완료를 표시하거나 폴링을 멈추지 않는다.
4. SUCCEEDED + execution.RUNNING: 모델 실행 중 표시, 같은 최종저장 GET 폴링을 계속한다.
5. SUCCEEDED + execution.COMPLETED: 폴링 종료. Case 목록/상태를 갱신하고 기존 결과 표시 흐름으로 연결한다.
6. SUCCEEDED + execution.FAILED: 폴링 종료. “저장 완료, 모델 실행 실패”로 표시하고 execution.failureReason을 사용한다. caseId/files를 보존하고 failure=null을 저장 실패로 해석하지 않는다. 최종저장·파일 업로드를 다시 호출하거나 자동 재실행하지 않는다.
7. SUCCEEDED + execution=null: 과거 완료된 저장으로 처리하고 폴링 종료. 저장 완료로만 표시하며 모델 실행 성공으로 간주하거나 자동 Run을 시작하지 않는다.

중복·재시도·화면 수명:
- 버튼 중복 클릭과 중복 폴링 루프를 막는다. 서버가 반환한 원래 operationId/requestId/draftId/Case 연결을 사용한다. 완료 재클릭 응답은 보낸 requestId와 다른 원래 requestId를 반환할 수 있다.
- POST 응답 유실/통신 오류는 실패 확정이 아니다. 접수 확인은 같은 requestId와 동일 body 재전송 또는 알려진 data.statusEndpoint 조회로 처리한다. 임의의 새 requestId로 저장을 다시 만들지 않는다.
- GET 오류나 FE 대기시간 경과도 서버 FAILED로 바꾸지 않는다. 기존 통신 오류 UX에 따라 상태 확인을 재개할 수 있게 하고, 재개 시 새 작업 생성 없이 기존 작업을 조회한다.
- 화면 이탈/언마운트/대상 변경 시 타이머와 불필요한 요청을 정리하고 늦은 응답이 다른 Draft/Case 화면을 덮어쓰지 않게 한다. 화면을 닫아도 BE 처리는 계속된다. 재진입은 기존 화면 상태 복원 방식과 저장된 작업 식별자로 GET을 재개한다.
- 저장 실패 후 사용자가 다시 저장할 때는 현재 Draft revision을 확인하고 새 requestId를 사용한다. 저장 성공 후 실행 실패는 최종저장 재전송으로 재실행되지 않는다.
- 실행 재시도는 기존 Case Run 기능을 사용자의 명시적 동작으로 연결한다. 먼저 최신 Case 상태를 확인하고 PENDING/FAILED에서만 POST /cases/{caseId}/runs를 호출한다. RUNNING이면 관찰을 이어가고 COMPLETED는 재실행하지 않는다. 전송 오류 후 무조건 자동 POST하지 않는다.
- 명시적 재실행 뒤에는 기존 Case/status 조회 흐름으로 새 Run을 추적한다. 최종저장 execution은 최초 연결 Run을 가리키므로 그 폴링을 재개해 새 Run의 결과를 기다리지 않는다.

완료 조건·검증:
- FE API 타입과 호출부, 상태 표시, 결과 이동, 오류/재시도 및 폴링 종료 조건을 위 계약에 맞게 수정한다. 미지원 표 편집·열 추가/삭제나 새 업로드 계약 구현으로 범위를 넓히지 않는다.
- 프로젝트의 기존 테스트 도구로 저장 QUEUED/RUNNING/FAILED, SUCCEEDED와 execution의 네 상태/null을 확인한다. 저장 진행 중 execution=null은 계속 조회하고, legacy 성공/null은 멈추는 차이를 검증한다.
- 실행 실패 시 저장 성공/Case 유지, 최종저장 SUCCEEDED에서 조기 종료하지 않음, 최초 자동 흐름의 Run POST 0회, 동일 요청 재전송, 200 재전송에서 진행 재개, 네트워크 오류, 이탈 정리/늦은 응답 무시, 명시적 재실행의 최신 Case 추적을 검증한다.
- 기존 API mock/타이머 테스트를 활용하고 필요한 타입 검사·빌드·관련 테스트를 실행한다. mock 검증을 실환경 모델 실행 성공으로 보고하지 않는다.

Work note:
- Stable path: docs/work-notes/2026/2026-10-19-input-draft-auto-execution-ui.md (frontend 저장소 기준).
- Action: create — 독립적인 FE 반영 목표. 이후 같은 목표의 후속 세션에서는 이 경로를 update한다.
- 변경 파일, 실행한 검증·결과와 실환경 연동 확인 여부를 기록한다.
```
