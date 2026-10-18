# MI 모델 Input 계산 엔진 실파일 Spike

## Document Control

- status: Complete
- canonical for: Reference only — INPUT-02 조사 및 INPUT-02A 선정 경로의 조건부 판정 근거
- owner: BE
- date: 2026-09-16
- task type: investigation
- source documents:
  - `docs/design-docs/gas/mi-model-input-editing-delivery-plan.md`
  - `docs/work-notes/2026/2026-09-03-model-input-transfer-format.md`
  - `docs/work-notes/2026/2026-10-07-input-02a-stage1-no-commercial-engine.md` (부록 A의 실행 기록)
  - `docs/references/mi-model-input-functions-2026-10-07.md` 및 JSON (최신 함수 전체 집계·기본 등록 근거)
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md`
  - `docs/references/mi-model-input-baseline-2026-10-07.md` (부록 A의 입력 기준본)
- supersedes: 없음
- last synced: 2026-10-15
- open questions:
  - F-S-06 제외 INPUT-02A는 2026-10-15 사용자 승인으로 `DONE (CONDITIONAL)`이다(Delivery Plan §6A). 계산/격리 비동기·운영 기준은 domain Q7/규칙63·64, 후속 API 전체 저장 응답·부분 실패 검증은 INPUT-04 계약/INPUT-07~09 구현/INPUT-10 검증을 따른다.
  - 지정 밖 함수 조합·이름 참조·VBA 동작과 일반 표의 계산/구조는 미검증이다. 선정19사례의 완료를 미지원 함수 계산 승인이나 전체 workbook 호환 인증으로 확대하지 않는다.

이 문서는 계산 엔진 선정 자체의 canonical 설계가 아니라, 실제 XLSM 3개로 수행한
`INPUT-02` 및 최신 source의 `INPUT-02A` 부분 실행 증거다. 셀 원문, 수식 원문, 고객사 식별값은 기록하지 않는다.

## INPUT-02A 최종 판정 — CONDITIONAL (2026-10-15)

- 사용자 최종 승인에 따라 F-S-06 제외 INPUT-02A를 **`DONE (CONDITIONAL)`**로 닫았다. 고정 source·선정19의 입력/보존·기록 범위 native Pass, 계산/동일변경 Excel9 Pass·계산 제외10 해당 없음, 승인7경로21회 자원·동시/중단6시험·평문 수명10시험·실제2vCPU/2GiB 대표12회 근거에 한정한다. 사례별 범위/상태는 [compatibility의 최종 표](mi-model-input-sheet-calculation-compatibility.md#input-02a-최종-조건부-완료--2026-10-15)를 따른다. 이전 전체 workbook POI/LibreOffice NO-GO는 유지한다.
- 승인 실행/운영·평문 정책은 domain Q7/규칙63·64, 티켓 판정과 INPUT-03/04/07 전달 제약은 [Delivery Plan §6A](../design-docs/gas/mi-model-input-editing-delivery-plan.md#6a-completed-ticket--input-02a-conditional-f-s-06-제외)를 따른다. [종료 기록](../../temp/input-02a-engine/2026-10-15-input-02a-conditional-closeout-c2mlcayi/final-summary.json)·[종료 감사](../../temp/input-02a-engine/2026-10-15-input-02a-conditional-closeout-c2mlcayi/verification.json)에 기존 증거·hash와 검증 한계를 연결했다. 별도 새 계산/골든/native/성능 시험은 실행하지 않았다.
- 조건/한계: 대표 제한환경 최대14.564초·worker kernel peak666.5MiB/OOM0은 warm cache·Linux/tmpfs 파일 처리 결과다. 각3회·역할별 값 합을 실제4파일 저장 요청/SLA로 해석하지 않는다. **API 전체 저장 응답은 INPUT-10 후속 PENDING**이며 INPUT-04 계약/INPUT-07~09 구현 후 대기열·DB/storage 저장 확정·완료/부분 실패·규칙64 시간 기준을 검증한다. OS reboot/정전·production worker 구현, 접근 불가 사용자 핸들·물리 매체 삭제, native 전수 계산/저장·VBA/모델 결과는 검증하지 않았다. 실제 DRM 재검증은 사용자 제외로 해당 없음이며 Pass가 아니다.
- 이 최종 절이 현재 판정이다. 아래 단계별 전체 In Progress/미승인 추천/남은 운영·DRM 결정 기록과 기존 실행 JSON은 최종 승인 전 이력으로 보존한다. 전체 기능/조사·제품 배포 완료는 후속 티켓과 구분한다.

## INPUT-02A 승인2vCPU·2GiB worker 대표 경로 재측정 — 2026-10-15

- [최종 결과](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/final-summary.json)·[별도 감사](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/verification.json)·[재현/경계](../../temp/input-02a-engine/2026-10-15-resource-limited-worker-0hv3nf5z/README.md): 사용자 승인 운영 기준은 domain 규칙64다. 이전 미승인 추천은 이 승인 전 이력이다. 별도 로컬 Linux worker를 한 번에1개만 실행하고 요청도 순차1건, 대표4경로 각3회·인정12회를 실제 제한에서 측정했다. Docker 설정과 내부 cgroup v2 모두 CPU `200000/100000=2vCPU`, 메모리 `2147483648bytes=2GiB`, swap0을 확인했다. JVM heap/CPU 인식값만 설정한 시험이 아니다.

| 사례 | 처리시간 중앙값(최소~최대), 초 | 관측 process group RSS peak, MiB | 승인 계산/보존 |
| --- | --- | --- | --- |
| F-D-03 | 5.747 (5.444~6.394) | 519.5 | 16합계 계산·기존 E:N 등 보존 |
| F-S-04-DELETE | 14.296 (13.431~14.564) | 490.2 | 삭제/이동 후6,264계산·기타 cache 보존 |
| F-T-04 | 3.187 (3.057~3.187) | 189.2 | 승인5입력 외 계산 제외·원본 보존 |
| F-O-04 | 0.280 (0.274~0.292) | 52.3 | 승인4입력 외 계산 제외·원본 보존 |

- worker 전체 cgroup의 커널 memory peak는 **666.5MiB/2GiB**, OOM kill0이다. 이 값은 controller·cache·tmpfs까지 포함하며 위 표의 표본 RSS와 지표가 다르다.12결과 모두 기존 승인 XLSM의 전체 SHA256·ZIP member/metadata·CRC/저장 후 재읽기 동일, source-only unit6 hash 동일, 실제6 JVM/18,840평가·실패0/skip0이다. 기존 입력/수식/구조·보존/외부 캐시 정책과 계산 도구/jar/class를 유지했고 새 golden/native·외부 링크/매크로/DRM은 실행하지 않았다. 동일 hash의 기존 Excel 비교·native 근거를 유지한다.
- 환경/시간 경계: 기존 로컬 Java 이미지에 Python 표준 라이브러리와 bundled openpyxl3.1.5·동일 POI5.4.0만 넣은 측정 전용 이미지다. Ubuntu24.04 arm64/Temurin17.0.18/Python3.12.3, 원본은 host 읽기 전용 bind, 새 작업 파일은2GiB에 계상되는512MiB tmpfs다. 이전 Mac/Homebrew17.0.14·파일시스템과 달라 시간 차이를 CPU 제한만의 영향으로 해석하지 않는다. 초기 source hash로 cache가 데워졌고 통제하지 않았다. 각3회 결과는 p95/최대 처리량/SLA가 아니다.
- 측정은 새 pipeline 시작→source 읽기/입력·구조/허용 POI→ZIP close/자식 종료까지다. 사후 승인 hash·보존 감사/정리는 별도 기록이며 이미지/컨테이너 시작·대기열/API·DB/storage upload·fsync는 제외한다. 각 역할 중앙값 합은 약23.511초이지만 같은4파일 최종저장 요청의 실측이 아니다. **30초 목표/전체120초 상한은 승인됐으나 그 전체 요청 SLA를 이 결과로 Pass 처리하지 않는다.** 후속 API/제품 검증에서 접수·대기·필요 저장 확정 경계를 확인한다.
- 실패/복구 이력: 첫 측정에서 D03/S04DELETE는 통과했고 F-T-04는 내부 검증용 baseline manifest의 읽기 전용 mount 누락으로 준비 단계에서1회 실패했다. OOM/계산 실패가 아니며 새 작업 파일/핸들·컨테이너 잔여0을 확인했다. manifest만 읽기 전용으로 추가하고 미완료 T04/O04의1회차, 이후 네 경로2/3회차만 실행했다. 최초 도구/실패 metadata와 인정12회/전체13시도를 구분해 보관했다. 원본/기존 검증2,610파일 hash 불변이다.
- 정리/상태: 새 XLSM·unit/results TSV·raw stdout/stderr는 검증 후 모두 삭제했고 token이 붙은 컨테이너도 제거했다. 각 종료 후60초 이내 파일/핸들0·실패 분석용 평문0이다. 컨테이너 PID namespace의 `/proc/*/fd`를 경로와 device/inode로 대조했으며 업무 내용은 이미지/build context/log에 넣지 않았다. 이번 제한환경 재측정은 Complete·전체 INPUT-02A In Progress, 최종 판정은 이번 요청에서 채택하지 않았다. 아래 미승인 추천/운영 대기는 이번 승인/실측 전 이력이다.

## INPUT-02A 잔여와 운영 추천 — 2026-10-15, 미승인 제안

- [최신 대조/추천 근거](../../temp/input-02a-engine/2026-10-15-input-02a-operating-recommendation-6bwaaahc/remaining-and-recommendation.json): F-S-06 제외 선정19파일의 입력/보존·native, 계산/Excel9·추가 계산 제외10,21회 자원 실측, 동시 요청/중단6시험, 평문 수명/프로세스 복구10시험은 완료했다. 실제 DRM 재검증은 사용자 제외다. 남은 것은 운영 예산·허용 대기/완료 경험·실행 경계 선택, 검증 한계를 명시한 최종 판정/후속 티켓 제약이다. 기존19파일 재실행/새 golden·DRM 표본을 요구하지 않는다.
- **미승인 추천:** 웹 요청 JVM과 별도인 프로세스/worker에서 비동기 처리한다. worker(파일 작업 실행 단위)당 초기 검증 예산2vCPU/2GiB, 동시 작업1·한 작업의 역할 파일은 순차 처리, 승인 case별 heap768MiB/1024MiB 유지다. 단일 요청 RSS 관측 최대570.8MiB와 기존1GiB heap 외 Python/native/VM 메모리 여유를 고려한 초기안이며, 해당 CPU/메모리 제한에서 성능을 실측한 운영 사양은 아니다. 동시4혼합987.0MiB/동일 SUPPLY2요청952.7MiB Pass를 최대 동시4 또는 무제한 메모리 안전 근거로 쓰지 않는다. 웹서버/OS의 기존 예산은 별도다.
- 저장 경험 제안은 정상 완료30초 목표·접수부터 대기열/처리/필요한 저장 확정까지 요청 전체120초 상한이다. 접수 응답과 저장 성공을 구분하고 진행/실패 상태를 표시하며 기존 실패 시 요청 전체 취소/직전 성공 Draft 보존 규칙을 따른다. 두 수치는 미승인 목표/한도 후보이며 관측한SLA·p95가 아니다. 실제 물리 저장/DB 완료·timeout 취소 경계는 INPUT-04/Q8c 후속 계약으로 구체화한다. 평문 정리 유예는 이미 확정된 domain 규칙63을 그대로 따른다.
- 근거/한계: 단일 파일7경로의 중앙값은0.223~11.858초·관측 최대12.027초다. 역할별 느린 중앙값 합19.693초는 **서로 다른 사례를 더한 추정**이고 같은4파일 최종저장 요청의 실측이 아니다. macOS10CPU/16GiB·OS cache 데워짐·각3회이며 DB/네트워크/storage 업로드·사후 보존 감사·fsync는 제외했다. 목표 환경/제한에서 대표 승인 경로를 좁게 재확인하는 것을 권장하며, 환경이 없으면 미검증 경계를 조건으로 수용/기록해 후속 제품 검증으로 전달한다. 이를 새 전체 production 조사 선행조건으로 확대하지 않는다.
- 소스 재확인: [AsyncConfig](../../src/main/java/com/sk/common/config/AsyncConfig.java:20)는 같은 JVM의 스레드 pool5/최대10·대기1000이며 프로세스 격리가 아니다. 이 값을 workbook 동시성/접수 정책으로 재사용할 근거는 없다. [Java17 공식 문서](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html)의 `-Xmx`는 heap 한도이며 VM 전체 메모리 한도가 아니다. [Kubernetes 공식 문서](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)의 CPU 제한/메모리 초과 종료 특성도 목표 제한에서 재확인을 권하는 근거다. 새 K8s Job/queue 기술·배포 구성을 이 추천으로 확정하지 않는다.
- 사용자 결정은 자원 예산/초기 동시1,30초 목표/120초 상한의 허용 여부, 격리 비동기/진행 표시, 검증된 계산/보존 경계와 운영 미검증 한계를 붙인 **CONDITIONAL 판정 후보** 수용 여부다. 확인 후 domain Q7·Delivery Plan §6A에 최종 경계/판정을 기록하고 INPUT-03의 domain/persistence, INPUT-04의 비동기 계약, INPUT-07의 실제 materialization/worker·정리 구현 제약을 넘긴다. Draft transaction/Case/Run/운영 E2E는 INPUT-08~10의 후속 검증이다. 이번에는 추천 정리만 Complete·전체 INPUT-02A In Progress이며 최종 판정/정책 채택은 하지 않았다.

## INPUT-02A 로컬 평문 작업사본 수명·삭제 — 2026-10-15

- [최종 결과](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/final-summary.json)·[검증](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/verification.json)·[재현/경계](../../temp/input-02a-engine/2026-10-15-plaintext-lifecycle-j210bq3l/README.md): domain 규칙63으로10시험(핸들 대조1·workload9)을 통과했다. 별도 사전 핸들 대조1도 통과했으며 이전 동시 요청 probe의 승인 pipeline을 새 요청별0700 scratch에 복사했다. 허용식·입력·writer는 유지하고 시험용 오류와 JVM 임시/error 경로만 추가했다. 원본/정규화/대장/기존 검증·domain/Delivery Plan 포함2,526자료 hash 불변이다.

| 사례 | 실제 조건 | 삭제·핸들0 확인까지, 초 | 결과 |
| --- | --- | --- | --- |
| H-01 | 실제 OUTPUT 사본을 읽기 핸들로 유지한 채 unlink; 경로·device/inode 대조 | 정리0.605, 열린 핸들1 검출 후 최종0 | Pass |
| L-01~04 | F-D-03·F-S-01·F-T-04·F-O-04 정상 처리 | 4.345 / 3.777 / 3.464 / 0.617 | 모두60초 이내 Pass |
| L-05 | F-D-03 준비/unit 생성 후 실제 예외 종료 | 0.629 | 60초 이내 Pass |
| L-06 | F-S-01 준비/unit 후 대기·시험 deadline12초에서 TERM | 0.622 | 60초 이내 Pass |
| L-07 | F-S-01 최종 ZIP 첫 member flush 중 SIGKILL; 열린 부분 파일 관측 | 0.659 | 60초 이내 Pass |
| R-01 | F-D-03 unit 단계에서 정리 controller SIGKILL·핸들8 잔존 후 새 recovery process | 복구 시작부터0.665 | 300초 이내 Pass |
| R-02 | F-O-04 완성/검증 후 정리 전 controller SIGKILL·핸들1 잔존 후 새 recovery process | 복구 시작부터0.637 | 300초 이내 Pass |

- 복구 중단 구간은 실제2.322/2.311초이며 옛 controller는 exit-9로 종료됐다. 외부 감사기는 그동안 파일을 지우지 않고 잔존 핸들을 관측한 뒤 새 복구 프로세스를 기동했다. 시간/mtime을 앞당기지 않았다. 종료 시간은 마지막 생존 관측/종료 신호부터 최종 핸들0까지의 보수적 상한이며, 복구 시간은 새 프로세스 기동 직전부터 계산해 startup/scan도 포함한다.60/300초 전에 실제 정리가 끝났으므로 기한 전체를 기다려 Pass로 만든 것이 아니다.
- 새 완성5파일(정상4+복구 직전 완성1)은 삭제 전에 승인 hash/ZIP CRC·모든 XML/rels 재읽기 동일을 확인했고, source-only unit6도 기존 승인 unit과 동일하다. 정상2 JVM의 실제9,628평가/실패0/skip0을 기록했다. 세 번째 JVM은 계산 반환 후 저장 중 KILL되어 완성 평가 기록/게시 성공에 합산하지 않았다. Java/JUnit 테스트는 미사용이다. 이전 동일 hash의 Excel 비교·보존/native 근거를 재사용하며 새 native/링크/매크로·DRM 호출은 없다.
- 모든 새 runtime/tools 입력 metadata·중간/완성 XLSM·unit/results TSV·stdout/stderr·VM 임시 사본을 삭제했다. 실제 임시 파일 mode0600, 요청 경로0700, 최종 파일/핸들/소유 그룹0·실패 분석용 평문 보관0이다. `lsof`는 같은 OS 사용자 중 열람 가능한 프로세스를 검사하고 경로와 device/inode 양쪽을 대조한다(최종 감사 경고0, 관측 시점별 프로세스 수는 verification.json). root/접근 불가 다른 사용자·저장장치 블록 삭제는 주장하지 않는다. 요청 token/파일 hash·크기·시각/상태만 새 증거로 남긴다.
- 이번 완료는 같은 호스트에서 controller/worker의 실제 프로세스 종료·새 프로세스 복구를 시험한 로컬 경계다. OS reboot/정전·production worker/복구 scheduler·Draft/권한/transaction 구현을 증명하지 않는다.12초 deadline/0.5초 TERM 유예·짧은 중단은 시험 제어값이다. 기존 기능의 실제 DRM 재검증은 사용자 제외, 전체 INPUT-02A는 In Progress이며 운영 자원/허용 저장 대기·완료 경험/실행 경계·최종 판정은 남는다.

## INPUT-02A 동시 요청 격리·timeout/강제 종료 후 정리 — 2026-10-15

- [최종 결과](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/final-summary.json)·[재현/경계](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/README.md): 직전 승인 pipeline·POI Java를 새 temp에 복사했다. 계산/입력·구조 writer는 유지하고, 재현용 checkpoint·TERM 무시 조건만 분리했다. 외부 probe 제어기가 요청별 private scratch(0700)·새 session/process group·검증 후 게시·그룹 종료/정리를 담당했다. 고정 정규화 source에서 독립 시작하며 과거 승인 결과는 저장 후 hash 비교에만 썼다.

| 시험 | 조건/관측 | 결과 |
| --- | --- | --- |
| C-01 four roles | F-D-03/F-S-04-DELETE/F-T-04/F-O-04 동시4요청, source/작업/결과 경로 분리, RSS 합계 관측987.0MiB | 4결과 승인 hash 동일·별도 파일/inode·scratch/소유 프로세스0, Pass |
| C-02 same SUPPLY | F-S-01 동시2요청, 서로 다른 JVM이 동시에 살아 있는128표본, RSS 합계 관측952.7MiB | 2결과 승인 hash 동일·unit/결과/프로세스 분리, Pass |
| T-01 ready timeout | source 준비/계산 unit 생성 후 test gate, 요청 시작12초 deadline에서 TERM | 준비 XLSM/unit 제거·실패 게시0·정상 동반 요청 완료, Pass |
| T-02 hung JVM timeout | 자체 JVM만 STOP, 시험 Python만 TERM 무시. 12초 deadline→TERM→0.5초 유예 후 KILL | 정지 JVM/소유 그룹 종료·실패 게시0·정상 동반 요청 완료, Pass |
| K-01 parent crash | pipeline 부모만 KILL, 남은 자식2개 관측 후 외부 제어기가 그룹 TERM/회수 | 고아 자식/작업 폴더0·정상 동반 요청 완료, Pass |
| K-02 partial ZIP kill | 최종 ZIP 첫 member662byte 저장/flush 후 gate, 그룹 KILL | 불완전 ZIP 미게시·제거·정상 동반 요청 완료, Pass |

- 6개 인정 시험/14요청 중 정상10파일은 [게시 검사](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/publication-verification.json)에서 기존 승인 결과와 hash/ZIP CRC·재읽기 동일이다. 실패 주입4요청의 게시 결과는0이고 앞서 게시된 성공 파일도 불변이다. 새 source-only unit11개는 기존 unit과 동일하다. [계산 기록](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/source-only-unit-and-evaluation.json): 완료8 JVM/35,164평가·실패0/skip0, 이 중 성공 게시7 JVM/25,552평가다. 나머지 완료9,612평가는 K-02에서 계산 후 저장을 중단했으므로 성공 결과로 게시하지 않았다. 주입으로 중단된2 JVM은 완료/정답/skip0을 주장하지 않고 별도 표시한다. JUnit/Gradle 테스트는 해당 없음이다.
- [모든 시도의 정리 검사](../../temp/input-02a-engine/2026-10-15-request-isolation-cleanup-xu06fygi/all-attempt-process-and-scratch-checks.json): 최초 상태 조회 차단과 종료 조건 보정으로 중단된4요청을 포함해 실제18요청의 소유 그룹·scratch가 모두0이다. 실패 주입4건은 신호 발생 후 약2.11~2.29초에 파일 정리까지 확인했으며 정상 동반 요청의 완료 확인/감사도 포함한 관측값이다. 삭제 전 파일 목록/크기/hash와 신호/PID·정리 후 증거를 남기고 실제 중간 XLSM/unit은 제거했다. 성공 최종 XLSM과 내용 없는 제어 파일/진단 로그는 지정 temp에 보관한다. 원본/보관본/정규화4역할·대장·기존2,122자료 및 domain/Delivery Plan은 불변이다.
- 재시도 이력: execution-v1의 앞3시험은 유지했다. T-02 첫 시도는 자체 JVM 상태를 읽는 `/bin/ps`의 sandbox 차단으로 중단·정리했다. 허용된 환경의 다음 시도에서는 STOP된 JVM까지 TERM으로 종료돼 KILL 전환 요구가 충족되지 않았다. 시험 Python에만 TERM 무시를 명시해 무응답 그룹을 재현한 뒤 execution-v3에서 미완료3시험만 Pass했다. 최초/2차 도구·로그/정리 기록을 보관했으며 이전 검증 자료를 고치지 않았다.
- 측정은 macOS/Java17/POI5.4.0의 제한된 로컬 probe다. RSS 표본20ms 목표/최대 간격75.566ms·공유 페이지 중복/짧은 peak 누락 가능성이 있고 gate 대기/실패 주입 시간은 사용자 저장 응답시간이나 생산 동시성/SLA 기준으로 쓰지 않는다. 12초/0.5초도 시험값이며 운영 정책이 아니다. 외부 제어기가 생존한 worker 종료/정리 경계만 확인했다. 제품 API/Draft revision·권한/transaction, 제어기·호스트 전체 유실 복구, DRM 평문 보안 삭제·실제 운영 부하는 미검증이다.
- 다음 확인은 DRM 복호화 표본/검증 가능한 대체 절차·평문 수명과 실제 운영 자원/허용 대기·완료 경험이다. 이를 반영한 동기/격리 비동기 경계 및 GO/CONDITIONAL/NO-GO·INPUT-03/04/07 전달 제약은 대기다. 이번 probe는 Complete·전체 INPUT-02A는 In Progress이며 native Excel/링크 갱신/매크로·제품 구현/배포는 실행하지 않았다.

## INPUT-02A 승인4역할7경로 읽기→저장 자원 실측 — 2026-10-15

- [최종 결과](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/final-summary.json)·[환경](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/environment.json)·[반복 원자료](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/measurements-v1/measurements.json)·[재현 명령](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/README.md): 고정 정규화4역할의 승인7경로를 각3회, 총21회 독립·순차 실행했다. 기존 준비 writer의 원본 읽기/입력·구조 변경 core와 기존 POI5.4.0 평가기를 새 temp에 복사해 연결했다. macOS27.0.1 arm64/10CPU/16GiB, Java17.0.14, Python3.12이며 계산마다 새 JVM이다. heap은 D03/S01 768MiB·S04DELETE1024MiB 설정을 유지했고 실제 메모리 측정과 구분한다.

| 사례 | 회당 계산 셀 | 처리시간 중앙값(최소~최대), 초 | 부모·자식 동시 RSS 관측 peak, MiB | 단일 프로세스 kernel peak, MiB |
| --- | --- | --- | --- | --- |
| F-D-03 | 16 | 4.659 (4.468~4.896) | 570.8 | 555.3 |
| F-D-05-DELETE | 0 | 5.141 (5.056~5.499) | 327.3 | 299.9 |
| F-S-01 | 9612 | 9.208 (9.034~9.329) | 524.1 | 351.5 |
| F-S-04-DELETE | 6264 | 11.858 (11.594~12.027) | 516.6 | 387.1 |
| F-T-04 | 0 | 2.461 (2.435~2.461) | 267.5 | 239.8 |
| F-O-01 | 0 | 0.234 (0.224~0.255) | 79.2 | 52.0 |
| F-O-04 | 0 | 0.223 (0.221~0.237) | 79.9 | 52.3 |

- 시간은 새 프로세스 시작→source 읽기/기존 source-local 검사·CRC→지정 입력/구조 반영→필요한 unit 새 추출→허용 POI→중간/최종 ZIP 저장·닫기→자식 종료까지다. 최초 protected hash·도구 준비/컴파일과 저장 후 hash/part/ZIP·정답/native 감사는 제외했다. 파일 저장은 OS buffered write/ZIP close이며 fsync·네트워크/DB/upload는 포함하지 않는다. 사전 hash 확인으로 OS 파일 캐시는 데워졌고 통제하지 않았다. 3회 표본은 production 부하·첫 디스크 읽기·서비스 JVM 상주 처리의 SLA/동시 처리량을 증명하지 않는다.
- MiB는1,048,576byte, RSS는 실제 메모리에 올라온 프로세스 페이지다. 외부 관측기가 Darwin `proc_pid_rusage`로 supervisor·실행 중 Python·Java를10ms 목표 간격으로 합산했다(실제 최대 간격32.485ms). 표의 동시 peak는 관측값이며 간격보다 짧은 peak를 놓칠 수 있고 공유 페이지가 중복 집계될 수 있다. 별도로 `wait4/getrusage`의 프로세스별 kernel 최대 RSS를 byte로 기록했다. 서로 다른 시간에 나온 개별 최대값 합계는 보수적 상한일 뿐 동시 최대값으로 사용하지 않는다. observer 메모리는 합계에서 제외했다.
- [unit 동등성](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/unit-equivalence.json): 새 source/변경에서 추출한 계산 입력9개가 기존 source-only unit과 byte/hash 동일이다. 실제47,676셀 평가·실패0·skip0, 허용식/상수·고정 기존 cache 경계 변경0이다. [저장 검사](../../temp/input-02a-engine/2026-10-15-approved-path-resource-nfg9e6_3/verification.json): 최종21파일 모두 기존 승인 결과와 hash/모든 ZIP part 동일·CRC/재읽기 Pass, 원본/보관본/정규화4역할·대장·이전1,706자료 불변이다. golden은 생성 입력으로 읽지 않았고 같은 hash의 기존 Excel 비교·보존/native 근거를 유지했다. Java는 standalone9회 실제 평가이며 JUnit/Gradle 테스트 실행은 해당 없음이다.
- 이 순차 실측 당시 남은 확인: 동시 요청의 자원/작업 경로 격리, timeout/kill·임시 파일 정리, DRM 표본/대체 절차와 평문 수명, 실제 운영 환경·허용 대기시간을 본 동기/격리 비동기 선택 및 GO/CONDITIONAL/NO-GO였다. 이후 위 로컬 동시 요청/worker 중단 정리 probe를 완료했다. 순차 실측에서는 native Excel/링크 갱신·매크로·동시 요청/실패 주입·DRM·제품 구현을 실행하지 않았다.

## INPUT-02A 4사례 선택 계산 부분 실행 — 2026-10-12

- Java 17.0.14 / Apache POI 5.4.0 기본 `FormulaEvaluator`, heap 768m, custom/fast SUMIFS 없음. 전체 XLSM을 POI로 열지 않고 승인 수식과 읽기 상수만 작은 메모리 workbook에 적재했다. 입력은 hash가 확인된 source/독립 미계산 복사본, golden은 저장 후 비교에만 사용했다.
- F-D-02 130셀, F-S-01 9,612셀, F-S-02/03 각 8셀을 실제 평가해 XLSM에 저장하고 다시 읽었다. 최종 총 9,758셀의 값·타입·오류와 확보된 Excel golden 비교 **Pass**, 네 생성 파일의 변경 전 정규화 source 기준 보존 **Pass**. 승인 수식은 그대로 유지했으며 계산 밖 셀·캐시/서식·validation·hidden·externalLink/캐시·pivot/캐시·names/merge 등 실제 항목을 확인했다. VBA/connection/native Table은 실행한 DEMAND/SUPPLY source에 없었다.
- Hurricanes는 source LUT 8캐시 셀로 외부 참조용 메모리 workbook을 만들었다. 최초 두 사례의 G8:G11·G16:G19는 evaluator map에서 주 평가기를 빠뜨려 `WorkbookNotFoundException`으로 실패했다. 주/LUT 평가기를 함께 등록한 뒤 두 사례만 독립 재실행해 각 8셀 Pass. 최초 로그/도구/진단 파일은 보존했다. 기존 G 캐시·0으로 대체하거나 수식을 다시 쓰지 않았고 외부 원본 개방·링크 갱신·매크로 실행도 없었다.
- [상세 범위/셀·보존 판정](mi-model-input-sheet-calculation-compatibility.md#4사례-poi-선택-계산-실행--2026-10-12), [최종 실행 결과·XLSM 경로/hash](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/final-summary.json), [환경/jar hash·설정](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/environment.json), [재현 명령·실패 이력·한계](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/README.md)를 연결한다. 실제 값·전체 비교/보존 JSON·도구/로그는 새 Git 제외 temp 경로에만 있다. Java/JUnit 테스트 대신 standalone 평가를 실행했다.
- 원본 전달 4파일·조사 대장·기존 표본/golden/기록 등 90개 경로 hash 불변과 생성 ZIP CRC·저장 후 재읽기 Pass. 2026-10-12 실행 당시 native 재개방은 **NOT_RUN**이었고, 후속 2026-10-13 결과/복사본 사용 한계는 다음 절을 따른다. 숫자 허용 오차는 기존 Excel 결과 기록을 그대로 사용했다.
- 이 결과는 네 사례의 선택 경로 검증이다. 아래 전체 workbook 로딩 `NO-GO`와 구분하며 INPUT-02A 전체 완료·제품 엔진 선정/배포·성능/동시성/DRM/운영 실행 방식 검증으로 확대하지 않는다. 나머지 사례/정책·구조·4역할 전체/모델 E2E는 후속이다. 실행 기록은 기존 stable work note를 update했다.

## INPUT-02A native Excel 재개방과 값 유지 확인 — 2026-10-13

- 최종 POI 산출물 네 파일을 Mac용 Microsoft Excel 16.113.4 (26100421)에서 직접 재개방·무저장 닫기했으며 복구 경고 미관측. SUPPLY 세 파일의 외부 링크 안내에는 `업데이트하지 않음`을 선택했다. 이후 사용자 승인 AppleScript로 동일 hash 읽기 전용 복사본의 Excel 내부 `value2`를 읽어 F-D-02 130/130·F-S-01 9,612/9,612·F-S-02/03 각 8/8, 총 **9,758셀 값 유지 Pass**를 확인했다. 모두 numeric·오류/누락/중복 0, 기존 절대/상대 1e-9 허용 오차 내 일치 및 보고 값의 float64 동일이다.
- 읽기 대상의 한계: writable 원산출물에 `read only true`가 적용되지 않아 F-D-02 첫 두 시도는 값 읽기 전에 중단·무저장 닫기했다. 원산출물과 동일 bytes/hash인 복사본에 0444 권한을 적용하고 실제 readOnly=true를 확인한 뒤 전체 값을 읽었다. **원산출물 직접 재개방 경고 확인과 읽기 전용 복사본의 전체 값 유지 확인을 구분한다.** 원산출물 자체의 AppleScript 전체 값 읽기·native 재계산/저장 cycle은 검증하지 않았다. Hurricanes는 `hurricanes-retry/` 최종 산출물을 사용했다.
- 실행 조건: 빈 helper를 통해 개방 전 수동 계산 모드·macro force disable을 고정하고 `update links do not update links`로 열었다. manual·forceDisable·readOnly·alerts=true를 확인하고 값/수식만 읽은 뒤 대상/helper를 `saving no`로 닫았다. 계산·값/수식 쓰기·링크/connection/pivot 새로고침·외부 workbook 개방·macro·save/save-as를 요청하지 않았고 기존 앱 설정을 복원했다. JSON/XML 숫자 표현의 차이 473셀은 최대 1e-15이나 보고 float64는 전부 같다. Hurricanes 16수식의 native 외부 경로 표기는 source에 이미 있던 대상이며 파일 수식·externalLink/캐시는 hash 불변이다. 입력/산출물 94경로·복사본 4개 hash 불변, 산출물 ZIP CRC·전체 XML/관계 parse Pass.
- [사례별 범위·수식 표기·보존 및 검증 한계](mi-model-input-sheet-calculation-compatibility.md#4사례-native-excel-재개방과-값-유지-확인--2026-10-13), [최종 native 값/설정 결과](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/native-summary.json), [동일 hash 복사본 manifest](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/read-only-copies-manifest.json), [실행·재현/실패 이력](../../temp/input-02a-engine/2026-10-12-poi-four-nysnm45q/native-reopen-2026-10-13-9pnqvvxo/applescript-x90usm_0/README.md)와 [stable work note](../work-notes/2026/2026-09-03-model-input-transfer-format.md#2026-10-13-생성-4사례-native-excel-무저장-재개방-결과)에 근거를 연결한다. 실제 값/로그는 Git 제외 temp에만 있다.
- 후속 읽기는 새 계산 성공·Excel golden 확보나 엔진 선정의 증거가 아니다. 최초 직접 UI 개방의 계산 모드는 자동이었고 내부 재계산 여부는 미확인이다. 이후 값 읽기에서는 별도로 개방 전 수동 계산을 고정했으며 내부 이벤트/네트워크 계측·전체 visual 비교·모델 실행은 수행하지 않았다. 나머지 6준비 사례·8미결정 명세·구조/4역할 전체·운영/DRM·엔진 선정은 남는다. **이 재개방 기록 시점에는 INPUT-02A BLOCKED·전체 조사 In Progress였고 이전 전체 workbook 경로 NO-GO는 유지한다.** 같은 날 이후 사용자 결정으로 POI 부분 계산이 승인되어 INPUT-02A는 `IN_PROGRESS`다(Delivery Plan §6A·§9). 이 승인은 `GO`가 아니다.

## 결론

**INPUT-02 당시 판정: NO-GO — 검증한 전체 workbook 경로의 Apache POI 또는 headless LibreOffice를 1차 임시저장 계산 엔진으로
채택할 수 없다.**

- Apache POI 5.4.0은 `supply.xlsm` 전체 로딩만으로 1 GiB heap을 소진했다. 수식 계산까지
  도달하지 못했다.
- 현재 서비스의 `setForceFormulaRecalculation(true)`는 다음 Excel 오픈 시 재계산을
  요청하는 플래그일 뿐, 저장 시점의 cached value를 갱신하지 않는다.
- 로컬 headless LibreOffice 일괄 round-trip은 완료됐지만, 수정된 cross-sheet 복합 수식의
  cached value가 갱신되지 않았다.
- LibreOffice round-trip은 세 파일에서 Excel package 구조를 바꾸었고, 특히 macro가 있는
  표본에서 VBA를 제거하고 external link 수도 변경했다.
- 따라서 화면 임시저장 응답에서 즉시 재계산된 테이블을 돌려주는 동기 처리로도 부적합하다.

당시 제안한 다음 최소 조치는 **Aspose.Cells for Java 평가 라이선스를 확보한 뒤 동일 probe를 그대로
재실행**하는 것이다. 실제 파일에서 계산 정확도와 보존성을 통과하기 전에는 production
엔진으로 선택하지 않는다. 위 2026-10-12 부분 실행은 상용 엔진 없이 수행했으며 후보 선정·전체 probe·운영 검증은 이번 범위 밖이다.
2026-10-13 사용자 결정으로 이 제안은 대체됐다. 상용 엔진은 평가하지 않고 POI 부분 계산을 후보로 승인했다(domain design §1A.4 Q7). 아래 「Aspose.Cells for Java」와 「다음 최소 조치」는 INPUT-02 당시 판단 이력이다.

## 검증 범위와 방법

대상 원본은 `scripts/demand.xlsm`, `scripts/supply.xlsm`, `scripts/trade.xlsm`이다.

1. 시작과 종료 시 원본 3개의 SHA-256을 비교해 원본 비변경을 확인했다.
2. `supply.xlsm` 복사본의 허용된 숫자 input 한 셀만 OOXML 수준에서 변경했다.
3. 다른 시트의 복합 수식 셀에 저장된 cached value가 변경 전 그대로임을 먼저 확인했다.
4. 후보 엔진으로 복사본을 열고 재저장한 뒤 해당 수식과 cached value를 다시 확인했다.
5. 원본과 결과물에서 시트 수, cell style 수, merged cell 수, data validation 수,
   defined name 수, external link 수, VBA binary hash를 비교했다.
6. `/usr/bin/time -l`로 wall time과 process peak RSS를 측정했다.

실행 probe:

`src/test/java/com/sk/common/utils/ModelInputCalculationEngineSpikeTest.java`

일반 test suite에서는 건너뛰며, 실파일 probe는 아래처럼 명시적으로 실행한다.

```bash
INPUT02_CALCULATION_SPIKE_ENABLED=true ./gradlew test -PforceTest \
  --tests 'com.sk.common.utils.ModelInputCalculationEngineSpikeTest' --info
```

## Apache POI 5.4.0

### 코드 경계

- 프로젝트 의존성: `build.gradle`의 `poi`, `poi-ooxml` 5.4.0.
- 현재 `ModelParamExcelMergeService.serialize()`는 workbook을 직렬화하기 전에
  `setForceFormulaRecalculation(true)`만 호출한다.
- Apache POI 공식 문서도 dependency 값 변경 후 cached result를 갱신하려면
  `FormulaEvaluator`를 실행해야 하며, force-recalculation flag는 Excel이 다음 오픈 시
  계산하도록 요청하는 별도 방식이라고 설명한다.
- POI evaluator는 미지원 함수에서 `NotImplementedException`이 발생할 수 있고,
  외부 workbook reference는 실제 참조 workbook과 evaluator map이 필요하다. VBA UDF는
  Java로 다시 구현해 등록해야 한다.

공식 근거:

- [Apache POI Formula Evaluation](https://poi.apache.org/components/spreadsheet/eval.html)
- [Apache POI Formula Evaluation 개발 가이드](https://poi.apache.org/components/spreadsheet/eval-devguide.html)
- [Apache POI License](https://poi.apache.org/legal.html)

### 실측

| 항목 | 결과 |
| --- | --- |
| 표본 | `supply.xlsm` |
| JVM heap | 1 GiB |
| 로딩 결과 | 실패 — `OutOfMemoryError` |
| 실패까지 wall time | 약 6.3초 |
| process peak RSS | 약 1.15 GiB |
| FormulaEvaluator 도달 | 못함 |

따라서 POI evaluator의 개별 함수 지원 여부보다 먼저 **전체 XSSFWorkbook 메모리 모델**이
현재 Backend 동기 처리 경계의 blocker다. heap을 늘려 로딩만 성공시키더라도, 약 186만 수식과
외부 reference를 evaluator가 Excel과 동일하게 처리한다는 증거는 없다.

### 라이선스·배포·운영

- 라이선스: Apache License 2.0. 기존 의존성이므로 신규 상용 라이선스 blocker는 없다.
- 배포: 별도 native binary는 필요 없다.
- 운영 blocker: 현재 1 GiB JVM에서 OOM. web request JVM에서 heap을 공유하면 동시 요청이
  서비스 전체 장애로 이어질 수 있다.
- 정확성 blocker: 미지원 함수, VBA UDF, 누락된 external workbook을 별도로 해결해야 한다.

**후보 판정: NO-GO.**

## Headless LibreOffice

### 실행 조건

- 로컬 실행 파일: `LibreOfficeDev 26.8.0.0.alpha0`.
- fresh user profile, `--headless`, `--convert-to "xlsm:Calc MS Excel 2007 VBA XML"`로
  원본 3개와 수정 복사본 1개를 한 process에서 처리했다.
- 이는 production 승인 버전이 아닌 개발 빌드 실측이다.

### 계산·성능 결과

| 항목 | 결과 |
| --- | --- |
| 입력 | 원본 복사본 3개 + 값 수정 복사본 1개 |
| wall time | 약 223.4초 |
| process peak RSS | 약 1.43 GiB |
| cross-sheet 복합 수식 보존 | 수식은 존재 |
| cached value 갱신 | 실패 — 변경 전 cached value 유지 |

단순 `--convert-to` round-trip은 명시적인 full calculation 계약이 아니며 이번 표본에서도
계산 결과를 갱신하지 못했다. 또한 3분을 넘는 처리 시간과 1 GiB를 넘는 RSS는 동기 임시저장
요청에 허용하기 어렵다.

### XLSM fidelity 결과

`true`는 비교한 구조 카운트 또는 binary hash가 같다는 뜻이다. 값/수식 원문은 비교 결과에
포함하지 않았다.

| 파일 | sheets | styles | merged | validations | names | external links | macro |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| demand | 유지 | 변경 | 변경 | 변경 | 변경 | 해당 없음 | 해당 없음 |
| supply | 유지 | 변경 | 변경 | 변경 | 변경 | 개수 유지 | 해당 없음 |
| trade | 유지 | 변경 | 변경 | 유지 | 변경 | **2 → 1** | **제거됨** |

LibreOffice 공식 문서도 Microsoft Office format의 복잡한 구조와 서식은 다르게 처리되거나
지원되지 않을 수 있다고 설명한다. VBA 원본 코드 보존은 user profile의 VBA 설정에 의존하며,
LibreOffice의 VBA 실행 호환성도 완전하지 않다.

공식 근거:

- [LibreOffice import/export 제한](https://help.libreoffice.org/latest/en-GB/text/shared/guide/ms_import_export_limitations.html)
- [LibreOffice VBA Properties](https://help.libreoffice.org/latest/en-US/text/shared/optionen/01130100.html)
- [LibreOffice Licenses](https://www.libreoffice.org/licenses/)

### 라이선스·배포·운영

- 라이선스: MPL 2.0 기반 Free Software. 설치 이미지의 전체 LICENSE/NOTICE와 포함 component
  라이선스 확인은 필요하지만 상용 runtime fee는 없다.
- 배포 blocker: JRE만으로 끝나지 않으며 LibreOffice native package, font, fresh profile,
  temp storage, process lifecycle을 포함한 별도 runtime image가 필요하다.
- 운영 blocker: 대형 native process의 timeout/kill, 동시성 제한, profile 격리, temp 정리,
  손상 파일 격리, sandbox가 필요하다.
- 보안 blocker: macro가 포함된 고객 파일을 처리하므로 macro 실행 비활성화와 외부 연결 차단을
  강제해야 한다.
- 정확성/fidelity blocker: 이번 기본 headless 경로에서 계산 실패, style·병합·name 변경,
  VBA 제거, external link 손실이 실제로 발생했다.

**후보 판정: NO-GO.**

## 그 밖의 후보

### Microsoft Excel desktop automation

Excel과 가장 가까운 계산 결과를 기대할 수 있어도 Backend server 후보로 두지 않는다.
Microsoft는 unattended/non-interactive server-side Office automation을 권장·지원하지 않으며,
불안정 동작과 deadlock 가능성을 명시한다.

- [Microsoft — unattended Office automation 고려사항](https://learn.microsoft.com/en-us/office/client-developer/integration/considerations-unattended-automation-office-microsoft-365-for-unattended-rpa)

### Aspose.Cells for Java

공식 문서는 XLSM load/save와 embedded formula calculation engine을 제공한다고 설명한다.
그러나 “대부분의 수식” 지원 주장만으로 Excel 호환성을 확정할 수 없고, 이 저장소에는 dependency와
license가 없어 실파일 PoC를 실행하지 않았다.

- [Aspose.Cells 계산 엔진](https://docs.aspose.com/cells/java/calculate-formulas/)
- [Aspose.Cells 지원 형식](https://docs.aspose.com/cells/java/supported-file-formats/)
- [Aspose Java API license types](https://purchase.aspose.com/policies/license-types)

라이선스는 개발자 수, 배포 위치, SaaS/API 제공 방식에 따라 달라진다. 평가 라이선스를 받은 뒤
실파일 계산·fidelity·성능을 확인하고, production 구매 유형을 법무/구매와 확정해야 한다.

**후보 판정: CONDITIONAL / UNVERIFIED.** 이 후보가 있다는 사실은 현재 작업의 NO-GO를
GO로 바꾸지 않는다.

## 다음 최소 조치

> 2026-10-13 대체: 아래 1~2단계(상용 엔진 평가)는 수행하지 않는다. 현재 후보는 POI 부분 계산이며 진행 상태와 검증 gate는 Delivery Plan `INPUT-02A`의 Done Criteria를 따른다. 아래 목록은 당시 기록으로 남긴다.

1. Aspose.Cells for Java 30일 평가 라이선스와 검토 가능한 EULA/견적 조건을 확보한다.
2. 이 문서와 동일한 3개 실파일 probe를 실행한다.
3. 다음 gate를 모두 만족할 때만 엔진 후보를 `GO`로 바꾼다.
   - 수정 input이 영향을 주는 cross-sheet 복합 수식의 cached value가 기대값으로 갱신된다.
   - macro binary, style, merged cell, validation, defined name, external link가 손실되지 않는다.
   - 결과 파일을 재오픈할 수 있고 원본은 변경되지 않는다.
   - 허용 가능한 시간·메모리 상한과 동시성 정책이 정해진다.
4. 통과하더라도 계산은 web request JVM 안이 아니라 자원 제한과 timeout을 둔 격리 worker로
   배치할지 운영 설계에서 결정한다. 이는 model container에 계산을 위임한다는 뜻이 아니다.

## 검증 한계

- LibreOffice는 로컬 개발 빌드 한 버전만 검증했다. stable 버전, 명시적 UNO `calculateAll`,
  VBA 보존 profile을 별도로 조합하면 결과가 달라질 수 있다. 다만 현재 기본 headless 경로는
  production 채택 증거로 사용할 수 없다.
- POI는 1 GiB에서 전체 로딩에 실패해 실제 sample FormulaEvaluator 정확도까지 도달하지 못했다.
- DRM 적용 파일은 이 probe에 포함하지 않았다. production에서는 복호화된 임시 파일의 수명,
  접근 권한, 삭제 보장도 별도 검증해야 한다.

## 부록 A. 2026-10-07 기준본 4파일 함수 조사 (INPUT-02A 1단계)

위 본문은 2026-09-16의 표본 3개 기준이고 판정(NO-GO)은 바뀌지 않는다. 이 부록은
[기준본 고정](mi-model-input-baseline-2026-10-07.md)한 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6 정규화본 4개에서
쓰이는 함수와 Apache POI 5.4.0의 계산 지원 여부를 정적으로 조사한 결과다. 다음 단계(미지원 함수 셀에서
거꾸로 따라가 재계산 대상 여부를 판정)의 입력이며, 이 부록은 그 판정을 하지 않는다.
수식·셀 값 원문은 기록하지 않고 함수 이름, 개수, 좌표 범위, 예/아니오 표지만 쓴다. 셀 단위 전체 목록은 Git에 넣지 않은
`temp/model-input-function-inventory/2026-10-07/function-inventory.json`에 있다.

전체 함수 집계·기본 등록 목록·source hash의 주 근거는 [최신 함수 reference](mi-model-input-functions-2026-10-07.md)와 그 JSON이다. 이 부록은 별도 조사의 교차 확인, 실제 함수 호출 시험, 배열 범위·판별대장 대응 근거를 보유한다. 업무 계산/보존 정책은 domain design, 검증 계획은 calculation compatibility 문서가 소유한다.

### A.1 결과 요약

- 대상은 4파일·70개 시트(숨김 14개 포함)의 수식 셀 1,863,766개 전부다. 시트별 수식·셀 수와 시트 part SHA-256이
  manifest와 모두 같다.
- 셀 수식에 쓰인 함수는 25종이다. POI 5.4.0 기준 **지원 24종, 미지원(이름은 알지만 구현 없음) 0종,
  목록 밖 1종**이다. 목록 밖은 `_XLFN.UNIQUE`이며 TRADE의 5셀에만 있다(아래 A.5).
- 조건부서식에는 목록 밖 `_XLUDF.ISFORMULA`가 16개 수식에 있다(SUPPLY 6개 시트 14, TRADE 2개 시트 2).
  셀 값 계산 대상이 아니라 별도 목록(A.6)으로만 둔다.
- 정의된 이름 94개의 정의식 및 이름 간 참조에서 함수 호출은 없다. 이름이 가리키는 일반 셀의 수식까지 추적한 결과는 아니므로 이름을 통한 계산 영향이 0이라고 판정하지 않는다.
- `INDIRECT`·`OFFSET` 호출은 셀·이름·조건부서식·유효성·표 열 수식 어디에도 없다.
- 이전 호환성 문서가 표준 목록 밖 호출 후보로 든 `GDP`·`PIPELINE`은 함수 호출이 아니다. 둘 다 문자열 리터럴 안의
  글자이며, 기존 스크립트의 함수 정규식을 기준본에 적용하면 서로 다른 수식 원문 기준 DEMAND `GDP` 1건·TRADE
  `PIPELINE` 6건이 같은 값으로 재현된다. 셀 수식에서 그 밖의 함수 이름은 기존 정규식과 새 추출기가 같고 기존 정규식의 누락은 없다.

### A.2 조사 대상

| 항목 | DEMAND | OUTPUT | SUPPLY | TRADE | 합계 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 시트 수 | 25 | 5 | 23 | 17 | 70 |
| 숨김 시트 | 3 | 0 | 6 | 5 | 14 |
| 수식 셀(`<f>`가 있는 셀) | 699,298 | 889 | 460,803 | 702,776 | 1,863,766 |
| normal | 132,913 | 1 | 186,371 | 102,976 | 422,261 |
| shared 원본 | 8,269 | 30 | 4,251 | 9,324 | 21,874 |
| shared 따라오는 셀 | 558,116 | 858 | 270,180 | 590,471 | 1,419,625 |
| array 시작 셀(dynamic array 포함) | 0 | 0 | 1 | 5 | 6 |
| 정의된 이름 | 37 | 18 | 12 | 27 | 94 |
| externalLink part | 0 | 1 | 1 | 2 | 4 |
| `vbaProject.bin` | 없음 | 없음 | 없음 | 있음 | - |

- shared 따라오는 셀은 XML에 수식 본문이 생략된 셀이다. 원본 수식의 함수를 따라 셀 수에 포함했다.
- array(dynamic array 포함)는 시작 셀만 `<f>`를 가진다. 적용 범위 안의 나머지 셀은 계산 결과 값이라 수식 셀 수에 들어가지 않는다.
- 숨김 시트는 DEMAND `Scenario Tracker`·`Sheet2_DISCARDAFTUSE`·`CubeDump_DISCARDAFTUSE`, SUPPLY `{USA}RigsLogic`·
  `{China}PolicyLogic`·`{USA}HurricanesLogic`·`RegasLogic`·`PipelineLogic`·`MonthlyAdjProduction`, TRADE `Sheet1`·
  `Industry research`·`net exporter`·`pipeline cap`·`pipeline contract`다. 전부 조사에 포함했다.

### A.3 방법과 검증

1. **입력 확인**: 4파일 정규화본·원본 snapshot의 SHA-256을 manifest와 작업 전후 모두 대조했고 같다. 판별대장도 전후 같다.
2. **방법 A (OOXML 직접 스캔)**: 시트 XML을 스트리밍으로 읽고, 직접 만든 수식 스캐너로 함수를 뽑았다. 스캐너는 문자열
   리터럴, 따옴표로 감싼 시트 이름(괄호 포함), 대괄호 안(외부 workbook 번호·구조적 참조)을 건너뛴다. shared 원본의 함수를
   따라오는 셀에 적용했다.
3. **방법 B (독립 재추출)**: `openpyxl` read-only 리더가 shared 따라오는 셀을 자체 번역 규칙으로 복원한 수식을 `openpyxl`
   수식 Tokenizer로 쪼개 함수를 뽑았다. 조건부서식·유효성은 방법 A와 다른 경로(원문 텍스트 정규식 + Tokenizer)를 썼다.
4. **대조 결과**: 함수별 사용 셀 수·호출 수·셀 주소 집합, `INDIRECT`·`OFFSET`·`#REF!`·외부 참조가 같은 셀에 있는 셀 집합,
   정의된 이름을 참조하는 셀 집합, 조건부서식·유효성 수식 수와 함수가 4파일 모두 **차이 0**이다. 유일한 차이는
   정의된 이름 개수(A 94 / B 72)이며, B(openpyxl)가 내장 이름 `_xlnm._FilterDatabase` 22개를 이름 목록에서 따로 처리하기
   때문이다. 이를 뺀 72개는 두 방법의 이름·범위가 같고, 내장 이름에는 함수가 없다.
5. **오탐 점검**: 기존 스크립트의 함수 정규식은 문자열 리터럴·괄호가 든 시트 이름·대괄호 안의 글자를 함수로 셀 수 있다.
   이번에는 새 추출기를 따로 쓰고, 같은 입력을 새 추출기·`openpyxl`·기존 정규식에 넣어 비교했다(23개 시험 수식에서 새
   추출기와 `openpyxl`이 모두 기대값과 일치, 기존 정규식은 9개 수식에서 가짜 함수 생성). 기준본 전체에 기존 정규식을
   적용하면 가짜 함수는 `GDP`·`PIPELINE` 둘뿐이다. 기존 스크립트는 수정하지 않았다.
6. **POI 판정**: 프로젝트 의존성 POI 5.4.0 jar에서 `Version.getVersion()`이 5.4.0임을 확인했다.
   `WorkbookEvaluator.getSupportedFunctionNames()` 251개, `getNotSupportedFunctionNames()` 161개를 조회해 이름 기준으로 세
   갈래(지원/미지원/목록 밖)로 나눴다. 이름 목록에는 접두어가 없다.
7. **실제 evaluate**: 기준본에서 쓰인 29종(셀 25 + 조건부서식 전용 4) 전부를 메모리 workbook에서 실제 `FormulaEvaluator.evaluate()`
   했다. 접두어 함수 2종은 원문 표기·대문자·접두어 제거 3가지 표기를 각각 인수 형태 31종으로 시도했고, 모든 시도가
   `NotImplementedFunctionException`이다. 접두어 없는 27종은 모두 evaluate에 도달했다(`AVERAGEIFS`·`MATCH`·`SEARCH`·
   `SUMIFS`·`VLOOKUP`은 범용 시험 인수에서는 Excel 오류값만 나왔다. 이름은 처리됐다는 뜻이며 계산 정확도는 이번 범위가 아니다).
   시험 도구 검증용 대조군: 접두어가 붙은 지원 함수 `_xlfn.STDEV.S`·`_xlfn.CONCAT`은 값을 반환, `_xlfn.IFS`·`_xlfn.XLOOKUP`은
   이름을 처리한 뒤 오류값을 반환, 존재하지 않는 이름과 미지원 목록의 `DATEDIF`는 `NotImplementedFunctionException`이다.
8. **함수 출처와 VBA 경계 (2026-10-08 정정)**: `UNIQUE`는 [Excel 내장 함수](https://support.microsoft.com/en-us/excel/functions/unique-function)이며 `_xlfn.UNIQUE`는 [MS-XLSX §2.2.2](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-xlsx/3d025add-118d-4413-9856-ab65712ec1b0)의 저장 문법에 포함된다. POI 목록 밖이라는 이유로 VBA 제공 주체 미확인으로 분류하지 않는다. 이 함수 출처 확인을 위한 `oletools` 설치는 선행조건이 아니다. `vbaProject.bin`은 TRADE에만 있으며 매크로 동작은 조사하지 않았다. 조건부서식의 `_xludf.ISFORMULA` 원본 표기·보존·평가 요구는 별도 검증한다.

### A.4 함수 목록과 실제 호출 시험의 사용 방법

함수별 사용 셀·호출 수·기본 등록 상태의 전체 표는 [최신 함수 reference](mi-model-input-functions-2026-10-07.md)의 「함수 전체 목록」을 사용한다. 이 부록에 같은 전체 표를 복제하지 않는다. 이름 경유 계산 영향 셀 수는 미검증이다.

A.3 7번의 실제 호출 시험은 다음과 같이 해석한다. 실제 업무 수식의 인수·함수 조합·Excel 정답을 검증한 결과는 아니다.

| 기본 등록 함수의 시험 결과 | 함수 수 | 해석 |
| --- | ---: | --- |
| 한 번 이상 값 반환 | 22 | 범용 시험 인수에서 계산 호출과 값 반환 확인 |
| 오류값만 반환 | 5 | `AVERAGEIFS`, `MATCH`, `SEARCH`, `SUMIFS`, `VLOOKUP`; 시험 인수에서 오류값을 반환했으며 실제 업무 인수의 정답 검증은 미완료 |

기본 등록이 없는 `UNIQUE`와 `ISFORMULA`는 원문/대문자/prefix 제거 표기 각각 31개 인수 형태에서 모두 `NotImplementedFunctionException`이었다. 셀 수식과 조건부서식의 사용 위치는 A.5/A.6을 참조한다. 이는 POI 5.4.0 기본 구현의 한계이며 사용자 정의 구현은 등록하지 않았다.

OUTPUT의 수식 889개에는 함수 호출이 없다. 직접 참조·산술 연산뿐이다. 이것은 재계산이 필요 없다는 뜻이 아니다.

### A.5 미지원·목록 밖 함수 셀의 위치

미지원(POI가 이름은 알지만 구현 없음)에 해당하는 함수는 없다. 목록 밖 함수의 셀 위치는 다음과 같다.

| 파일 | 시트(상태) | 수식 범위(시작 셀) | 종류 | 함수 | 같은 셀의 다른 함수 | 외부 workbook 참조 | `#REF!` | `INDIRECT`·`OFFSET` | 저장값 오류 | 대장 표 ID |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TRADE | `MidstreamContractInput` (표시) | `CK4:CM10` (`CK4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-137 · 재확인 필요 |
| TRADE | `MidstreamContractInput` (표시) | `CQ4:CS29` (`CQ4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-138 · 재확인 필요 |
| TRADE | `backup_MidstreamContractInput` (표시) | `CK4:CM14` (`CK4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-187 · 재확인 필요 |
| TRADE | `backup_MidstreamContractInput` (표시) | `CQ4:CS29` (`CQ4`) | array · dynamic | `_XLFN.UNIQUE` | `CHOOSE` | 없음 | 없음 | 없음 | 아니오 | T-187 · 재확인 필요 |
| TRADE | `Industry research` (숨김) | `A3` (`A3`) | array · dynamic | `_XLFN.UNIQUE` | - | 없음 | **있음** | 없음 | **예** | T-188 · 재확인 필요 |

- 5곳 모두 `MidstreamContractInput`·`backup_MidstreamContractInput`·`Industry research`의 array(dynamic array) 수식의 시작 셀이다. 표의 「수식 범위」는 XML이 기록한 적용 범위이며, 시작 셀 외의 셀은 계산 결과 값이라 수식 셀로 세지 않았다. 적용 범위를 모두 합치면 211셀이다(TRADE array 5개 기준).
- 대장 표 ID는 판별대장 「표 대장」 H열(해석 범위)이 이 수식 범위를 완전히 포함하는 표다. 부분 겹침은 없었다. TRADE의 대장 범위는 v3 기준이라 모두 「재확인 필요」다. 어느 표에도 속하지 않는 셀은 없다.
- 4곳은 외부 workbook 참조·`#REF!`·`INDIRECT`·`OFFSET`이 없고 저장값도 오류가 아니다. 숨김 시트 `Industry research!A3`만 `#REF!`를 포함하고 저장값이 오류다.
- `UNIQUE`는 Excel 내장 함수다. `_xlfn.` 저장 표기와 POI 5.4.0의 기본 구현 부재를 구분한다(A.3 8번).
- 2026-10-08 정책 대응: 사용자 확인 및 최신 대장의 T-137/T-138/T-187/T-188 A 정책에 따라 위 5셀은 모두 원본 보존이다. `MidstreamContractInput!CK4/CQ4`가 있는 미표시 표는 수식·배열 결과·cached value를 유지하고 재계산하지 않는다. 상세 보존 범위는 [함수 reference](mi-model-input-functions-2026-10-07.md)를 따른다. 위 표의 표시/숨김은 시트 상태이지 표의 화면 표시 정책이 아니다. POI 기본 미지원이라는 기술 사실은 유지하지만 현재 UNIQUE 구현을 계산 선행조건으로 두지 않는다.
- 이 5곳 외에 SUPPLY `LiqProdAdjustment_backup`의 array(dynamic) 1셀이 있지만 `IFERROR`·`INDEX`·`MATCH`만 쓰는 지원 함수 수식이다.

### A.6 셀 수식 외의 수식 위치 — 조건부서식·데이터 유효성·Table 계산열

다음 표는 조건부서식 수식의 함수다. 수식 수는 해당 함수를 쓰는 수식의 수, 호출 수는 등장 횟수다. 조건부서식 평가와 데이터 유효성 검사는 일반 셀 값 재계산과 별도 책임이다. Table 계산열은 실제 셀 값의 계산식이므로 metadata 위치에서 검출했다는 이유로 재계산 대상에서 제외하지 않는다. 대응 셀의 계산/보존 정책과 구조 변경 시 수식 복제 규칙에 연결한다.

| 함수 | 파일 표기 | POI 목록 판정 | 실제 evaluate | DEMAND | SUPPLY | TRADE | 수식 수 합계 | 호출 수 합계 |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| **`ISFORMULA`** | `_XLUDF.ISFORMULA` | **목록 밖** | **NotImplementedFunctionException** | - | 14 | 2 | 16 | 16 |
| `AND` |  | 지원 | evaluate 도달(구현됨) | - | 14 | 2 | 16 | 16 |
| `ISNUMBER` |  | 지원 | evaluate 도달(구현됨) | - | 10 | 2 | 12 | 18 |
| `NOT` |  | 지원 | evaluate 도달(구현됨) | 2 | 14 | 2 | 18 | 18 |
| `OR` |  | 지원 | evaluate 도달(구현됨) | - | 5 | 2 | 7 | 7 |
| `ROW` |  | 지원 | evaluate 도달(구현됨) | - | 3 | 2 | 5 | 5 |
| `ISERROR` |  | 지원 | evaluate 도달(구현됨) | 2 | - | - | 2 | 2 |
| `SEARCH` |  | 지원 | evaluate 도달(구현됨) | 2 | - | - | 2 | 2 |

조건부서식 수식은 37개(DEMAND 20·SUPPLY 14·TRADE 3)이고, 함수가 든 수식의 적용 범위는 다음과 같다. 확장(x14) 조건부서식은 없다.

| 파일 | 시트(상태) | 함수 | 적용 범위 |
| --- | --- | --- | --- |
| DEMAND | `WeatherInput` (표시) | `ISERROR`, `NOT`, `SEARCH` | `AY9:AY1016` |
| SUPPLY | `BCRegasification` (표시) | `AND`, `ISNUMBER`, `NOT`, `_XLUDF.ISFORMULA` | `G8:XFD1048576` |
| SUPPLY | `LiqProdAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `_XLUDF.ISFORMULA` | `B11:C1048576`, `D11:F1048576`, `G7:XFD8`, `G11:XFD1048576` |
| SUPPLY | `LiqUtilAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `_XLUDF.ISFORMULA` | `B11:C1048576`, `D11:F1048576`, `G7:XFD8`, `G11:XFD1048576` |
| SUPPLY | `ProductionAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `I8:K1048576`, `M8:M1048576` |
| SUPPLY | `RegasAdjustment` (표시) | `AND`, `ISNUMBER`, `NOT`, `_XLUDF.ISFORMULA` | `H9:XFD10`, `H11:XFD1048576` |
| SUPPLY | `{USA}Hurricanes` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B13:D1048576` |
| TRADE | `LNG_TradeRestrictionInput` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B6:J1048576` |
| TRADE | `PNG_TradeRestrictionInput` (표시) | `AND`, `ISNUMBER`, `NOT`, `OR`, `ROW`, `_XLUDF.ISFORMULA` | `B6:I1048576` |

- 데이터 유효성 수식은 14개(DEMAND 7·SUPPLY 3·TRADE 4)이며 함수를 쓰는 수식은 없다. 확장(x14) 유효성은 없다.
- 표(Table) 계산열 수식은 TRADE `4.5 PriceModelInput`에 4개(적용 표 범위 `B3:D11`·`K15:P23`·`R3:U70`)이고 `IFERROR`(2회)·`INDEX`(6회)·`MATCH`(6회)·`SUMIF`(3회)만 쓰며 모두 지원 함수다. pivot 계산 필드·항목 수식은 없다.
- 이름 정의 94개(내장 `_xlnm._FilterDatabase` 22개 포함)의 수식에는 함수가 없다.

### A.7 한계

- **정적 조사**다. VBA가 실행 중에 하는 일과, `INDIRECT`·`OFFSET`처럼 문자열로 만들어지는 참조는 따라가지 못한다. 이번 기준본에는 두 함수 호출이 없지만, 그 사실이 다른 방식의 간접 참조가 없다는 증거는 아니다.
- POI 판정은 **이름 목록과 최소 1회 evaluate**다. 인수 형태·함수 조합에 따른 계산 정확도, Excel과의 값 일치, dynamic array의 결과 shape는 보지 않았다. evaluate는 실제 workbook이 아니라 메모리의 임시 셀에서 했다.
- 전체 XLSM을 POI로 로딩·재저장하지 않았다. 본문의 1 GiB heap OOM 판정과 LibreOffice 보존 실패 결과를 이 조사가 해소하지 않는다.
- 「저장값 오류」는 셀에 오류 타입의 저장값이 있는지만 본다. 오류의 의미나 재계산 시 달라질지는 판정하지 않는다.
- 조사 위치는 셀 수식, 이름 정의, 조건부서식, 데이터 유효성, 표 계산열, pivot 계산 필드·항목이다. TRADE의 Power Pivot 데이터 모델(`xl/model/item.data`)의 수식(DAX)과 연결·쿼리 정의, 외부 link의 저장 값은 조사하지 않았다.
- 대장 표 ID는 판별대장의 해석 범위(H열)로만 맞췄다. DEMAND·OUTPUT·TRADE의 대장은 v3 범위 기준이라 v8 대응은 후속 검증이 필요하다.
- 이름 정의식·이름 간 참조만 조사했으며 이름→일반 셀→수식 의존성은 추적하지 않았다. 이름 경유 영향 셀 수는 미검증이다.
- VBA 모듈 동작은 미검증이다. 이는 Excel 내장 `UNIQUE`의 출처 확인과 별도이며, POI 함수 지원·파일 보존·모델 영향 검증을 대신하지 않는다.
