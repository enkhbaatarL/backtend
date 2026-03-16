# MI 플랫폼 DB 오브젝트 명명 규칙 (PPTX 정리본)

- 원본: `/Users/hoduhomil/Downloads/download41SEU140237/1.MI플랫폼_DB오브젝트관리명세서_20250613.pptx`
- 추출일: 2026-03-12
- 목적: ERD/DDL 정의 시 바로 참조 가능한 명명 규칙 요약

## 1. 공통 원칙

- 데이터 표준 용어 사전에 정의된 단어/약어를 사용한다.
- 컬럼 속성명은 데이터 용어 사전에 등록된 명칭만 사용한다.
- 모든 오브젝트명은 소문자를 사용한다.
- 약어 연결은 `_`를 사용한다.
- 일련번호가 필요한 경우 `2자리`(`01`부터 증가)를 사용한다.
- 이름 길이가 DBMS 제약을 초과하면 의미를 유지하는 최소 약어를 사용한다.

## 2. 리소스 구분 코드

| 대상 | 코드 | 비고 |
|---|---|---|
| Table | `t` | Table 또는 Synonym |
| View | `vw` | View/MView |
| Trigger | `tr` | Trigger |
| Procedure | `pr` | Stored Procedure |
| Function | `fu` | Function |
| Index (일반) | `ix` | Secondary Index |
| Index (Unique) | `ux` | Unique Index |
| Index (FullText) | `fx` | FullText Index |
| Foreign Key | `fk` | FK 제약/인덱스 식별 |
| Partition RANGE | `ptr` | partition range |
| Partition LIST | `ptl` | partition list |
| Partition HASH | `pth` | partition hash |
| Partition KEY | `ptk` | partition key |

## 3. 테이블 명명 규칙

### 3.1 패턴

- 기본 패턴: `{resource(1)}{subject(2)}_{business_name}{entity_type(1)}`
- 예시: `tmm_mrktprfncm`

### 3.2 주제영역 코드

| 주제영역 | 코드 |
|---|---|
| 공통 | `cm` |
| 기준정보 | `md` |
| MI 모델 | `mm` |
| 인터페이스 | `if` |

### 3.3 엔터티 유형 코드

| 유형 | 코드 | 의미 |
|---|---|---|
| 기본 | `m` | 비즈니스 주체/대상 기본 정보 |
| 상세 | `d` | 상위 엔터티 1:N 상세 정보 |
| 내역 | `l` | 거래/작업 시점 누적 정보 |
| 통계 | `s` | 통계/집계 정보 |
| 이력 | `h` | 변경 이력 보관 |
| 송신 | `t` | 타 시스템 송신 목적 |
| 수신 | `r` | 타 시스템 수신 목적 |
| 변경 | `c` | 원천 데이터 가공/변경 정보 |

## 4. 테이블 종속/관련 오브젝트 명명 규칙

### 4.1 View

- 패턴: `vw_{table_name}{nn}`
- 예시: `vw_tmm_mrktprfncm01`

### 4.2 Trigger

- 패턴: `tr_{table_name}{nn}`
- 예시: `tr_tmm_1000m01`

### 4.3 Procedure / Function

- 패턴(권장): `{pr|fu}_{system_or_subject}_{meaning_name}{nn}`
- 예시: `pr_tmm_makemrktstepl01`
- 비고: 의미명은 표준 약어 조합으로 작성하며 30자 이내를 권고한다.

### 4.4 Index / FK

- 패턴: `{table_name}_{ix|ux|fx|fk}{nn}`
- 예시: `tmm_mrktprfncm_ix01`
- 비고: PK 인덱스명은 MySQL에서 `PRIMARY`로 자동 설정된다.

### 4.5 Partition

- 패턴: `{ptr|ptl|pth|ptk}_{partition_scope}`
- 예시: `ptr_202202`, `ptr_max`

## 5. ERD 정의 시 함께 적용할 테이블 설계 규칙 (요약)

- 컬럼 순서: `PK -> 시스템 컬럼 -> 일반 컬럼`
- 시스템 컬럼 5종(전 테이블 공통):
  - `reg_user_id`, `reg_dtm`, `mod_user_id`, `mod_dtm`, `del_yn`
- PK 설계:
  - PK 없는 테이블 생성 지양
  - 복합 PK는 조회 패턴 기준 선두 컬럼 배치
  - PK 컬럼 수는 가급적 4개 이하 지향
- FK 설계:
  - 참조무결성이 필요한 테이블은 FK 제약 설정
  - 인터페이스 수신 목적 테이블은 FK 제외 가능
- 파티션 설계:
  - 파티션 키는 반드시 PK/UK 일부 컬럼이어야 함
  - 파티션 유형에 맞는 데이터 타입 지원 여부를 사전 확인

## 6. 실무 적용 체크리스트

- 새 테이블명은 `{t}{주제2자리}_{업무약어}{엔터티유형1자리}` 패턴을 따르는가
- 엔터티 유형(`m/d/l/s/h/t/r/c`)이 데이터 성격과 일치하는가
- View/Trigger/Procedure/Function/Index/FK/Partition 이름이 규칙 패턴과 일치하는가
- 동일 테이블 내 다중 오브젝트의 일련번호가 `01`부터 순차 증가하는가
- 컬럼/약어가 데이터 표준 용어 사전과 일치하는가

## 7. 확인 필요 항목

- Trigger 규칙 설명 문구의 `tb_` 제외 표기는 원문 레이아웃상 문맥 확인이 필요하다.
- FK를 독립 오브젝트로 볼지, 인덱스 계열(`_fkNN`)로 통일할지 운영 기준 확인이 필요하다.
- Procedure/Function의 시스템구분 2자리 코드 체계(문서 내 `업무구분코드`)는 운영 표준 목록 확인이 필요하다.
