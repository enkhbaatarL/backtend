# 모델 입력 기준본 고정 — 2026-10-07

## Document Control

- status: Complete
- canonical for: Reference only — 사용자 지정 최신 4파일의 원본/정규화 식별과 package 보존 검증 근거
- owner: BE
- source documents:
  - 사용자 확인: 2026-10-07, 10월 6일 전달본 DEMAND·OUTPUT·TRADE v8 / SUPPLY v6 선택
  - `mi-model-input-baseline-2026-10-07-manifest.json`
- depends on:
  - `docs/design-docs/gas/mi-model-input-template-catalog-domain-model-design.md` §6 규칙 29~31·39~44
- supersedes: 실행 검증 source 선택으로는 판별대장의 DEMAND·OUTPUT·TRADE v3 / SUPPLY v6 묶음을 대체. 기존 대장 정책·관측 이력은 보존한다.
- last synced: 2026-10-07
- open questions:
  - v3 판별대장의 표 범위·색·수식 관측과 v8 실파일의 대응은 후속 표별 검증에서 확인한다.
  - native Excel 재개방·계산 정답·모델 실행 E2E는 이 기준본 고정 단계에서 수행하지 않았다.

## 고정 결과

기준 묶음 ID는 `2026-10-07-delivery-261006-v8-v6`다. Downloads의 `플랫폼 입력 수정본_261006`에 있는 네 파일을 직접 읽고 SHA-256을 확인했다. 원본 파일은 변경하지 않았으며, 동일 bytes의 읽기 전용 source snapshot과 정규화본을 로컬에 별도 보관했다.

정규화는 각 파일의 `SUMMARY`, `플랫폼 입력 안내` 두 시트만 제거했다. 개발본 78개 시트에서 8개를 제거해 70개를 유지한다. 업무 worksheet의 값을 읽어 다시 저장하거나 수식을 재계산하지 않았다.

| 역할 | 원본 버전 / 원본 SHA-256 | 정규화 SHA-256 | 유지 시트 / 수식 |
| --- | --- | --- | ---: |
| DEMAND | v8 / `73fa29ff6fa19652243479a43754a65076364488120a69657178c8311b6ce0bc` | `ef516b6de2a829c891dbd991ba9f9d626f104dcae2e5ecd8128b1f0ae31c23bd` | 25 / 699,298 |
| OUTPUT | v8 / `fa8bbda8ffa3e3aac1ce0767d403c9ab51f94806b286535e8de7878d416e9080` | `69742cc9c6fba170324c09c21986574a390c741ba26d371acdce570aeaac28ce` | 5 / 889 |
| SUPPLY | v6 / `c5ab906edf807dbf2707a9e13d7146a92c7490f054fed1b7d3d42afd4312824e` | `eefbcde6e6a5baeb3491bc2d37abe79a0e9a0ff511d1673ddee39f46ea64bd36` | 23 / 460,803 |
| TRADE | v8 / `8274fc25be1a89dac8e57eda64d84593576d7a71524d4b46de035da300cf6f8e` | `b1211662aa00d49d9f03c1d732acd495cf99446ede69abde34565007b3819a31` | 17 / 702,776 |

유지 수식 합계는 1,863,766개다.

## 보관 위치

- 정규화본: `temp/model-input-baselines/2026-10-07-delivery-261006-v8-v6/{DEMAND,OUTPUT,SUPPLY,TRADE}-normalized.xlsm`
- 동일 bytes 원본 snapshot: 위 폴더의 `source/{ROLE}-source.xlsm`
- 입력/출력의 정확한 경로·SHA-256, sheet 이름/ID/상태/part hash·수식 수, 제거/변경 part와 검증 결과: [manifest](mi-model-input-baseline-2026-10-07-manifest.json).
- XLSM은 업무 데이터이므로 Git에 넣지 않는다. 기존 `.gitignore`의 `temp/`를 사용하며 파일 권한은 읽기 전용 `0400`이다. 위 파일은 후속 계산 검증의 입력으로 보존한다.

## 정규화와 검증 경계

XLSX import/export 도구는 XLSM의 VBA·externalLink·pivot·원본 package bytes 보존을 보장하지 않으므로 ZIP/OOXML package를 직접 처리했다. 소유 관계가 있는 개발 전용 part만 제거하고 나머지 part는 그대로 복사했다.

변경 허용 metadata는 `xl/workbook.xml`, `xl/_rels/workbook.xml.rels`, `[Content_Types].xml`, 시트 목록을 가진 `docProps/app.xml`이다. workbook의 개발 시트·해당 defined name을 제거하고 잔여 localSheetId를 원래 업무 시트에 대응하도록 이동했다. worksheet sheetId는 바꾸지 않았다. 전용 printerSettings·시트 relationship도 제거하고 공유 part는 유지했다.

| 검증 | 결과 |
| --- | --- |
| 원본과 source snapshot SHA-256, 작업 전후 원본 불변 | 네 파일 모두 일치 |
| ZIP CRC·개방, 내부 relationship 신규 누락 | Pass / 신규 누락 0건 |
| 업무 worksheet 70개 bytes, 기존 셀·수식·저장값·서식 | 원본과 동일 |
| 그 밖 유지 part 238개 | 원본과 동일; VBA, externalLinks, connection, pivot, styles, sharedStrings, calcChain 포함 |
| defined name의 소속 sheet와 식, workbook 계산 설정 | 동일; localSheetId만 대응 index로 이동 |
| 개발 시트의 수식 및 업무 수식의 개발 시트 직접 참조 | 각각 0건; Excel 수식 엔진 검증을 대신하지 않음 |
| 별도 openpyxl read-only 개방, 4파일 sheet 목록 | Pass; 25 + 5 + 23 + 17 = 70 |
| native Excel / 재계산 / 모델 E2E | 미수행 — 후속 계산 검증 단계 |

재계산과 셀·레이아웃 편집을 수행하지 않았고 retained worksheet·style·drawing 및 관련 bytes가 동일하므로 별도 화면 rendering은 생략했다. source/normalized 전체 ZIP bytes가 같은 것은 아니다. package entry가 제거되고 workbook metadata가 바뀌므로 정규화본의 해시는 별도로 관리한다.

## 판별대장과 후속 검증

SUPPLY v6 원본은 대장 SHA-256과 일치한다. DEMAND·OUTPUT·TRADE v8의 업무 worksheet part는 대장 v3 대비 각각 25·5·17개에서 bytes가 다르다. 이 차이만으로 모든 셀 값이 달라졌다고 판단하지 않는다. 수식 합계가 같아도 표 좌표·색·참조/저장값·편집 정책 대응이 같다는 증거가 아니다.

대장의 사용자 확인 정책은 domain design에 유지한다. 이전 좌표와 representative fixture는 세 역할의 v8 대응 검증 후 사용한다. 검증용 source bytes/hash는 이 단계에서 고정됐으며, 표별 정책 정렬·계산 정답·저장 API·DB 확정은 후속 단계다. 이 결과는 운영 template DB 등록이나 고객사 운영 승인 완료를 뜻하지 않는다.

## 재현 절차

스크립트: `scripts/normalize-model-input-baseline.py`. 입력 JSON은 역할별 `role`, 고정 원본 snapshot `path`, `sha256` 네 항목이다. manifest의 `sourceSnapshotPath`/`sourceSha256`으로 입력 JSON을 만들고 새 출력 폴더·새 manifest 경로를 지정한다.

```bash
python3 scripts/normalize-model-input-baseline.py \
  --inputs <고정 원본 snapshot 4개를 지정한 JSON> \
  --output-dir <새 로컬 비추적 폴더> \
  --manifest <새 manifest 경로>
```

이미 존재하는 출력 폴더·manifest는 덮어쓰지 않는다. 해시가 다른 원본, 개발 시트 수식·업무 수식의 개발 시트 직접 참조, 새 package relationship 누락이나 유지 part bytes 변경은 실패로 처리한다.
