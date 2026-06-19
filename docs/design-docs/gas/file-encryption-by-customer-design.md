# 특정 고객사 대상 파일 암복호화 설계

## Document Control

- status: Draft
- canonical for: Business rules
- owner: BE
- source documents:
  - `docs/conventions/source-of-truth-governance.md`
  - `docs/work-notes/2026/2026-06-16-file-crypto-single-file-hook-implementation.md`
  - `docs/work-notes/2026/2026-06-19-file-crypto-zip-download-design.md`
  - `docs/work-notes/2026/2026-06-19-file-crypto-admin-company-context.md`
- depends on:
  - 외부 암복호화 서비스 API 계약
- supersedes:
  - 없음
- last synced: 2026-06-19
- open questions:
  - ZIP 대상 엔트리 암호화 적용 전 최대 파일 크기, 최대 파일 수, 동시 요청 제한을 운영값으로 확인해야 한다.

> 대상: SKGAS 고객사 / MS Office 문서 확장자
> 작성 맥락: 특정 고객사와의 파일 전송 구간 암호화 적용

## 1. 요구사항

특정 고객사(SKGAS)에 한해, MS Office 문서 확장자 파일을 대상으로 **전송 구간(클라이언트 ↔ 서버) 암호화**를 적용한다.

- **업로드**: 클라이언트가 *암호화된 파일*을 전송 → 서버가 **복호화** → Object Storage에 **평문 저장**
- **다운로드**: Object Storage에서 평문 read → 서버가 **암호화** → 클라이언트에 **암호문 전달**
- **ZIP 다운로드**: Object Storage에서 각 파일 평문 read → 대상 Office 파일만 **개별 암호화** → 암호화된 엔트리를 ZIP에 포함하여 클라이언트에 전달

즉, 저장소(at-rest)는 현재와 동일하게 평문을 유지하고, **특정 고객사와의 경계에서만** 암복호화가 일어난다. 이렇게 하면 내부 처리(정형데이터 파싱 등)는 모두 평문 그대로 동작한다.

### 적용 조건 (AND)

| 조건 | 값 |
| --- | --- |
| 고객사 | 유효 고객사 코드가 `SKGAS` |
| 확장자 | MS Office (`xlsx, xls, xlsm, docx, doc, docm, pptx, ppt, pptm`) |

두 조건을 **모두** 만족할 때만 암복호화를 수행한다.

유효 고객사 코드는 다음 우선순위로 판단한다.

1. 요청 header `X-Company-No`가 있으면 해당 `customerCompanyNo`로 조회한 `customerCompanyCd`
2. `X-Company-No`가 없으면 JWT claim의 `companyNo`로 조회한 `customerCompanyCd`

따라서 Admin 사용자가 자신의 JWT `companyCd`가 `SKGAS`가 아니더라도 `X-Company-No`로 SKGAS 고객사 컨텍스트를 선택해 파일 업로드/다운로드를 수행하면 암복호화 대상이 된다. 반대로 `X-Company-No`가 없으면 기존처럼 로그인 사용자의 회사 컨텍스트 기준으로 판단한다.

## 2. 현재 구조 (적용 지점)

| 구분 | 위치 | 비고 |
| --- | --- | --- |
| 업로드 단일 길목 | `FileService.upload()` (`src/main/java/com/sk/domain/shared/service/FileService.java:186`) | 모든 업로드 경로(`uploadFile`, `uploadStructuredFile`, `uploadFileToRelationship` 등)가 수렴 |
| 다운로드(단건) | `FileService.downloadFile()` (`...FileService.java:262`) | `byte[]` 반환 |
| 다운로드(ZIP) | `FileService.downloadFiles()` (`...FileService.java:289`) | 여러 파일을 한 zip에 압축 |
| 저장소 I/O | `ObjectStorageService` (`...ObjectStorageService.java`) | NCP Object Storage (S3 호환) |
| 고객사 식별 | `AuthorizationService.getCompanyCdWithPriority()` | `X-Company-No` 우선, 없으면 JWT `companyNo` |
| 기존 암호화 자산 | `AESUtil` (`src/main/java/com/sk/common/utils/AESUtil.java`) | 문자열/내부 값 보호용 기존 유틸. 이번 파일 암복호화 연동에는 사용하지 않음 |

> 참고: 이번 요구사항의 파일 암복호화는 MI Platform 내부 AES 유틸로 직접 처리하지 않고, 외부 암복호화 서비스 API에 위임한다.

## 3. 설계

핵심 원칙: `if (고객사 && 확장자)` 분기를 컨트롤러/서비스 곳곳에 흩뿌리지 않고 **정책 컴포넌트 한 곳**으로 모은다. 업로드, 단건 다운로드, ZIP 다운로드의 외부 전달 경계에서 같은 정책을 사용한다.

### 3.1 설정 (환경변수/시크릿 주입)

`application.yml` — 대상 목록·확장자는 설정으로 관리한다. 암복호화 API endpoint는 profile별 환경변수로 주입하며 실제 URL, 인증 정보, 고객사별 민감 값은 repository에 기록하지 않는다.

```yaml
file:
  encryption:
    enabled-company-cds: SKGAS                                  # 콤마 구분, 확장 가능
    target-extensions: xlsx,xls,xlsm,docx,doc,docm,pptx,ppt,pptm  # MS Office

external:
  file-crypto:
    base-url: ${FILE_CRYPTO_BASE_URL:}
    upload-path: ${FILE_CRYPTO_UPLOAD_PATH:}
    status-path: ${FILE_CRYPTO_STATUS_PATH:}
    encrypt-path: ${FILE_CRYPTO_ENCRYPT_PATH:}
    decrypt-path: ${FILE_CRYPTO_DECRYPT_PATH:}
    connection-timeout: ${FILE_CRYPTO_CONNECTION_TIMEOUT:3s}
    read-timeout: ${FILE_CRYPTO_READ_TIMEOUT:30s}
    retry-count: ${FILE_CRYPTO_RETRY_COUNT:1}
    retry-backoff-ms: ${FILE_CRYPTO_RETRY_BACKOFF_MS:500}
    pid: ${FILE_CRYPTO_PID:}
    label-id: ${FILE_CRYPTO_LABEL_ID:}
    user-id: ${FILE_CRYPTO_USER_ID:}
```

`@ConfigurationProperties`로 바인딩하여 대상 고객사·확장자·외부 API endpoint와 암복호화 서비스 호출 식별값을 코드에서 분리한다. 추후 다른 고객사 추가 시 **코드 변경 없이 설정 변경**으로 대상 조건을 확장한다.

기본 운영값:

| setting | value |
| --- | --- |
| `connection-timeout` | `3s` |
| `read-timeout` | `30s` |
| `retry-count` | `1` |
| `retry-backoff-ms` | `500` |

### 3.2 `FileEncryptionPolicy` (단일 진실 공급원)

```java
@Component
@RequiredArgsConstructor
public class FileEncryptionPolicy {

    private final FileEncryptionProperties props;   // enabled-company-cds, target-extensions

    /** 유효 고객사 + 확장자 두 조건을 모두 만족할 때만 true */
    public boolean isEnabled(String effectiveCompanyCd, String fileName) {
        return effectiveCompanyCd != null
            && props.getEnabledCompanyCds().contains(effectiveCompanyCd)
            && hasTargetExtension(fileName);
    }

    private boolean hasTargetExtension(String fileName) {
        if (fileName == null) return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = fileName.substring(dot + 1).toLowerCase();   // 대소문자 무시
        return props.getTargetExtensions().contains(ext);
    }
}
```

- "SKGAS인지", "Office 확장자인지" 판단이 **이 클래스 한 곳**에만 존재한다.
- `FileEncryptionPolicy`에 전달하는 회사코드는 `AuthorizationService.getCompanyCdWithPriority()` 또는 업로드 호출부에서 이미 해석한 유효 고객사 코드여야 한다.
- 인증 컨텍스트가 없는 SYSTEM/비동기 흐름에서는 `companyCd == null` → 자동으로 `false`.

### 3.3 외부 암복호화 API 연동 흐름

암호화와 복호화 모두 외부 암복호화 서비스의 파일 처리 API를 같은 순서로 호출하되, 업로드 후 실제 암호화 여부를 먼저 확인해 이중 암복호화를 방지한다.

1. 암복호화 서비스의 **파일 업로드 API** 호출
2. 암복호화 서비스의 **암호화 여부 확인 API** 호출
3. 상태 확인 결과에 따라 암복호화 서비스의 **암호화 또는 복호화 API** 호출 여부 결정
4. 암호화/복호화 API를 호출한 경우 암복호화 서비스의 **파일 다운로드 API** 호출

여기서 업로드/다운로드 API는 MI Platform의 기존 `FileService.upload()` / `downloadFile()`이 아니라, 암복호화 서비스가 제공하는 별도 API이다.

| 단계 | MI Platform 역할 | 암복호화 서비스 역할 |
| --- | --- | --- |
| 1. upload | 암호화/복호화 대상 파일 바이너리를 암복호화 서비스 업로드 API로 전송 | 작업 대상 파일을 임시 보관하고 file id/token 등 작업 식별자 반환 |
| 2. status | 1단계 식별자로 암호화 여부 확인 API 호출 | 업로드된 파일이 암호화돼 있으면 `true`, 아니면 `false` 반환 |
| 3. encrypt/decrypt | 상태 확인 결과상 필요한 경우에만 암호화 또는 복호화 API 호출 | 암호화/복호화 작업 수행 후 결과 파일 식별자 또는 다운로드 식별자 반환 |
| 4. download | 3단계 결과 식별자로 암복호화 서비스 다운로드 API 호출 | 결과 파일 바이너리 반환 |

#### Upload API 계약

| 항목 | 값 |
| --- | --- |
| method | `POST` |
| configured path | `external.file-crypto.upload-path` (`FILE_CRYPTO_UPLOAD_PATH`) |
| effective path | `{upload-path}/{pid}` |
| Content-Type | `multipart/form-data` |
| multipart file field | `file` |

Path example:

| setting | value |
| --- | --- |
| `upload-path` | `/api/v1/mi/upload` |
| `pid` | `1234-s3df-d7f7-d88` |
| effective upload path | `/api/v1/mi/upload/1234-s3df-d7f7-d88` |

Response fields:

| field | type | 의미 |
| --- | --- | --- |
| `result` | `int` | 처리 결과 |
| `errorMsg` | `string` | 실패/오류 메시지 |
| `fid` | `UUID` (`guid`) | 업로드된 파일 식별자. 이후 status/encrypt/decrypt API 호출에 사용 |

Upload result rule:

| case | `result` | `fid` | `errorMsg` |
| --- | --- | --- | --- |
| success | `1` | 생성됨 | `""` |
| failure | `2` | 없음 | 생성됨 |

Example:

```bash
curl -F "file=@/path/aaa/aaa.xlsx" ...
```

```java
byte[] result = fileCryptoApiClient.decrypt(fileBytes, originalFileName);
```

#### Encryption Status API 계약

| 항목 | 값 |
| --- | --- |
| method | `GET` |
| configured path | `external.file-crypto.status-path` (`FILE_CRYPTO_STATUS_PATH`) |
| effective path | `{status-path}/{fID}/{pID}` |
| request fields | 없음 (`fID`, `pID`는 path segment로 전달) |
| response fields | `Result`, `ErrorMsg` |

Response fields:

| field | type | 의미 |
| --- | --- | --- |
| `Result` | `boolean` | 업로드된 파일이 암호화돼 있으면 `true`, 암호화돼 있지 않으면 `false` |
| `ErrorMsg` | `string` | 실패/오류 메시지 |

Status decision rule:

| operation | `Result=true` | `Result=false` |
| --- | --- | --- |
| upload decrypt | 복호화 API 호출 후 결과 다운로드 | 복호화 API를 호출하지 않고 원본 업로드 바이트 그대로 저장 진행 |
| download encrypt | 암호화 API를 호출하지 않고 원본 바이트 그대로 응답 | 암호화 API 호출 후 결과 다운로드 |

> 구현은 응답 필드 `Result`/`ErrorMsg`를 기준으로 매핑한다. 기존 소문자 `result`/`errorMsg`가 내려와도 방어적으로 수용한다.

#### Encrypt API 계약

| 항목 | 값 |
| --- | --- |
| method | `POST` |
| Content-Type | `application/x-www-form-urlencoded` |
| request fields | `OperationUserID`, `LabelID`, `FID`, `PID` |
| response fields | `OutputFilePath`, `DataSize`, `Status`, `ErrorMsg`, `OperationUserID`, `FID`, `LabelID` |

Encrypt status rule:

| case | `Status` | `ErrorMsg` |
| --- | --- | --- |
| success | `0` | `null` |
| failure | `1` | 생성됨 |

#### Decrypt API 계약

| 항목 | 값 |
| --- | --- |
| method | `POST` |
| Content-Type | `application/x-www-form-urlencoded` |
| request fields | `UserID`, `FID`, `PID` |
| response fields | `OutputFilePath`, `DataSize`, `OperationUserID`, `Status`, `ErrorMsg`, `FID` |

Decrypt status rule:

| case | `Status` | `ErrorMsg` |
| --- | --- | --- |
| success | `4` | `null` |
| failure | `5` | 생성됨 |

#### Result Download 계약

| 항목 | 값 |
| --- | --- |
| method | `GET` |
| URL 조합 | `base-url + OutputFilePath` |
| 별도 download path 설정 | 불필요 |
| response field | `content` |
| response type | `bytearray` |


> 정정: encrypt request field는 `LabelID`로 전달하고, encrypt response field도 `LabelID`로 수신한다. 이전 문서의 잘못된 표기는 오타다.

> 현재 확보한 curl 예시 기준으로는 별도 인증 header 없이 호출한다. `ErrorMsg`는 별도 에러 객체가 아니라 단순 `string` 필드다. 업로드 파일과 결과 파일은 당일에만 사용 가능하다.

### 3.4 길목 훅 — MI Platform 업로드

`FileService.upload()`에서 storage put 직전. `companyCd`는 호출부에서 `X-Company-No` 우선순위까지 반영한 유효 고객사 코드로 전달되어야 한다. 파일명은 `file.getOriginalFilename()` 사용(내용은 암호화돼도 **파일명은 평문**이므로 확장자 판별 가능).

적용 대상이면 클라이언트가 보낸 암호문 파일을 바로 Object Storage에 저장하지 않고, 외부 암복호화 서비스에 업로드한 뒤 복호화 API와 결과 다운로드 API를 순서대로 호출한다. MI Platform Object Storage에는 복호화된 평문만 저장한다.

```java
if (policy.isEnabled(companyCd, file.getOriginalFilename())) {
    byte[] decrypted = fileCryptoApiClient.decrypt(file.getBytes(), file.getOriginalFilename());
    objectStorageService.uploadFile(new ByteArrayInputStream(decrypted),
            bucketName, fileName, isPublic, file.getContentType(), decrypted.length);
    fileSize = (long) decrypted.length;   // ★ DB에는 복호화(평문) 크기 저장
} else {
    objectStorageService.uploadFile(file, bucketName, fileName, isPublic);
    fileSize = file.getSize();
}
```

> 핵심: 업로드 후 DB의 `fileSize`(및 해시)는 **평문 기준**으로 저장해야 다운로드 검증·내부 처리와 일관성이 유지된다.

### 3.5 길목 훅 — MI Platform 다운로드

`FileService.downloadFile()`에서 기존 `fileNm` + `fileSize` 검증을 먼저 수행한다. 이때 `fileSize`는 DB에 저장된 **평문 파일 크기** 기준이며, SKGAS 클라이언트도 상세/목록 API에서 받은 `fileNo`, `fileNm`, `fileSize`를 그대로 다운로드 요청에 전달한다.

검증 통과 후 Object Storage에서 평문 파일을 읽고, 요청자 고객사와 확장자가 암호화 대상이면 외부 암복호화 서비스에 업로드한 뒤 암호화 API와 결과 다운로드 API를 순서대로 호출한다. MI Platform은 암호문을 임시 Object Storage에 다시 업로드하지 않는다. 암호문 산출물의 저장, 수명, 정리는 암복호화 서비스 책임이다.

```java
byte[] data = objectStorageService.downloadFile(bucketName, filePath);
String companyCd = authorizationService.getCompanyCdWithPriority();   // X-Company-No 우선
if (policy.isEnabled(companyCd, uploadedFile.getOriginalFileNm())) {
    data = fileCryptoApiClient.encrypt(data, uploadedFile.getOriginalFileNm());
}
```

#### 다운로드 책임 경계

| 구분 | 책임 |
| --- | --- |
| MI Platform | 다운로드 권한 및 `fileNo`/`fileNm`/`fileSize` 검증, Object Storage 평문 파일 조회, 암복호화 서비스 upload/status/encrypt/download 호출, 클라이언트 다운로드 응답 반환 |
| 암복호화 서비스 | 암복호화 대상 파일 임시 보관, 암호화/복호화 처리, 결과 파일 다운로드 제공, 산출물 만료/정리 정책 |

> 결정: 암호화/복호화 API가 파일 바이너리를 직접 동기 응답으로 반환하는 모델이 아니라, 암복호화 서비스의 별도 upload/download API를 조합하는 모델로 본다.

### 3.6 길목 훅 — MI Platform ZIP 다운로드

`POST /api/v1/file-management/download`는 여러 파일을 하나의 ZIP으로 압축해 반환한다. 이 경로도 SKGAS 요청자가 MS Office 확장자 파일을 다운로드하는 외부 전달 경계이므로 암호화 적용 대상이다.

정책은 **ZIP 컨테이너 전체 암호화가 아니라, ZIP 내부 대상 파일 엔트리의 개별 암호화**다.

처리 순서:

1. 요청 body의 각 `fileNo`, `fileNm`, `fileSize`를 기존과 동일하게 DB의 `tcm_filel` 저장값과 대조한다.
2. 검증 기준의 `fileSize`는 업로드 시 저장된 **평문 파일 크기**이며, 암호화 결과 크기와 비교하지 않는다.
3. 각 파일을 Object Storage에서 평문으로 읽는다.
4. 요청자 고객사와 원본 파일명이 `FileEncryptionPolicy` 대상이면 해당 파일 byte를 외부 암복호화 서비스로 암호화한다.
5. 암호화된 byte를 원본 파일명 그대로 ZIP 엔트리에 쓴다.
6. 비대상 고객사 또는 비대상 확장자 파일은 평문 byte를 ZIP 엔트리에 쓴다.
7. 하나의 ZIP 안에 대상/비대상 파일이 섞이면 대상 Office 엔트리만 암호화하고 나머지 엔트리는 기존처럼 평문으로 포함한다.

```java
String requesterCompanyCd = authorizationService.getCompanyCdWithPriority();
for (UploadedFile file : uploadedFiles) {
    byte[] entryData = objectStorageService.downloadFile(bucketName, filePath);
    if (policy.isEnabled(requesterCompanyCd, file.getOriginalFileNm())) {
        entryData = fileCryptoApiClient.encrypt(entryData, file.getOriginalFileNm());
    }
    zos.putNextEntry(new ZipEntry(file.getOriginalFileNm()));
    zos.write(entryData);
    zos.closeEntry();
}
```

#### ZIP 다운로드 결정 이유

| 대안 | 결정 |
| --- | --- |
| ZIP 전체를 암호화 | 채택하지 않음. 암호화 대상 조건이 Office 확장자 기준이며, ZIP 자체는 Office 문서가 아니다. 대상/비대상 파일이 섞인 경우에도 과도하게 전체 ZIP이 암호화된다. |
| ZIP 내부 Office 엔트리만 개별 암호화 | 채택. 단건 다운로드와 동일하게 클라이언트가 받는 Office 파일 경계에서만 암호화된다. |

#### ZIP 다운로드 구현 경계

| 구분 | 책임 |
| --- | --- |
| `FileDedicatedController.downloadFiles(...)` | 인증된 요청자의 유효 고객사 코드를 조회하고 ZIP 다운로드 서비스 호출에 전달 |
| `FileService.downloadFiles(...)` | 파일 metadata 검증, Object Storage 평문 조회, 대상 엔트리 암호화, ZIP 생성 |
| `FileEncryptionPolicy` | 요청자 고객사 + 원본 파일명 확장자 기반 대상 판단 |
| `FileCryptoApiClient` | ZIP 엔트리에 쓰기 전 대상 파일 byte 암호화 |

`Content-Disposition`의 ZIP 파일명과 HTTP response `Content-Length`는 최종 ZIP byte 기준이다. ZIP 내부 각 엔트리명은 기존 원본 파일명을 유지한다. `tcm_filel.file_size`와 클라이언트가 요청에 전달하는 `fileSize`는 계속 평문 파일 크기 기준이다.

대상 파일 암호화 중 외부 암복호화 서비스 오류가 발생하면 ZIP 다운로드 전체를 실패 처리한다. 대상 파일만 평문으로 우회해서 ZIP에 넣는 fallback은 전송 구간 암호화 요구사항을 깨므로 허용하지 않는다.

## 4. 컴포넌트 / 변경 목록

| 항목 | 종류 | 비고 |
| --- | --- | --- |
| `FileEncryptionProperties` | 신규 | `@ConfigurationProperties(prefix="file.encryption")` |
| `FileEncryptionPolicy` | 신규 | 대상 고객사/확장자 판단 단일 진실 공급원 |
| `FileCryptoApiProperties` | 신규 | `@ConfigurationProperties(prefix="external.file-crypto")` |
| `FileCryptoApiClient` | 신규 | 암복호화 서비스 upload/status/encrypt/decrypt/download API 연동 |
| `FileService.upload()` | 수정 | 업로드 복호화 훅, `fileSize` 평문 기준 저장 |
| `FileService.downloadFile()` | 유지 | 단건 다운로드 metadata 검증 및 Object Storage 평문 조회 |
| `FileDedicatedController.downloadFile()` | 수정 | 외부 전달 직전 단건 다운로드 암호화 훅 |
| `FileDedicatedController.downloadFiles()` | 수정 | 요청자 회사코드를 ZIP 다운로드 처리로 전달 |
| `FileService.downloadFiles()` | 수정 | ZIP 내부 대상 Office 엔트리를 압축 전 개별 암호화 |
| `AuthorizationService.getCompanyCdWithPriority()` | 신규/사용 | Admin의 `X-Company-No` 고객사 컨텍스트를 암호화 정책에 반영 |
| `directDownloadFiles()` | 보류 | URL 직접 다운로드는 별도 설계 후 적용 |
| `application-*.yml` | 수정 | `file.encryption.*`, `external.file-crypto.*` 추가 |
| 환경변수/시크릿 | 추가 | 암복호화 서비스 base URL, endpoint path, 인증 정보 |

## 5. 구현/운영 확인 항목

1. **암복호화 서비스 API 세부 계약**
   암호화와 복호화 모두 `암복호화 서비스 upload API → status API → 필요한 경우 encrypt/decrypt API → 필요한 경우 암복호화 서비스 download API` 순서로 호출한다.
   → Upload API는 `POST` + `FILE_CRYPTO_UPLOAD_PATH/{FILE_CRYPTO_PID}` + `multipart/form-data` + multipart file field `file` + 응답 필드 `result:int`/`errorMsg:string`/`fid:UUID(guid)`로 확정이며 성공은 `result=1`, 실패는 `result=2`다. Status API는 `GET` + `FILE_CRYPTO_STATUS_PATH/{fID}/{pID}` + response `Result:boolean`, `ErrorMsg:string`으로 확정이며 `Result=true`는 암호화됨, `Result=false`는 암호화되지 않음을 의미한다. Encrypt API는 `POST` + `application/x-www-form-urlencoded` + request `OperationUserID`, `LabelID`, `FID`, `PID` + response `OutputFilePath`, `DataSize`, `Status`, `ErrorMsg:string`, `OperationUserID`, `FID`, `LabelID`로 확정이며 성공은 `Status=0`, 실패는 `Status=1`이다. Decrypt API는 `POST` + `application/x-www-form-urlencoded` + request `UserID`, `FID`, `PID` + response `OutputFilePath`, `DataSize`, `OperationUserID`, `Status`, `ErrorMsg:string`, `FID`로 확정이며 성공은 `Status=4`, 실패는 `Status=5`다. 결과 파일 다운로드는 `GET` + `base-url + OutputFilePath` + response `content:bytearray`로 확정이다. 현재 확보한 curl 예시 기준으로는 별도 인증 header 없이 호출한다. 업로드 파일과 결과 파일은 당일에만 사용 가능하므로, MI Platform은 같은 요청 흐름에서 후속 호출을 즉시 완료해야 한다.

2. **암복호화 서비스 파일 수명/정리 정책**
   암복호화 서비스 upload/download API가 임시 파일을 관리하며, 업로드 파일과 결과 파일은 **당일에만 사용 가능**하다. 따라서 MI Platform은 upload -> status -> 필요한 경우 encrypt/decrypt -> 필요한 경우 download를 하나의 요청 흐름 안에서 즉시 수행해야 하며, 다음 날 재사용이나 지연 처리 전제를 두면 안 된다.

3. **용량/타임아웃 정책**
   암복호화 서비스에 파일을 업로드한 뒤 다시 결과를 다운로드하므로 MI Platform 요청 처리 시간이 증가한다. 최대 파일 크기, connect/read timeout, 재시도 정책, 동시 호출 제한을 구현 전에 확정해야 한다.

## 6. 결정 사항 (확정)

- 적용 대상 고객사: 유효 고객사 코드가 `SKGAS`
- 유효 고객사 코드: `X-Company-No`로 조회한 고객사 코드 우선, 없으면 JWT `companyNo`로 조회한 고객사 코드
- 적용 확장자: MS Office 문서 (`xlsx, xls, xlsm, docx, doc, docm, pptx, ppt, pptm`)
- 암복호화 방식: MI Platform 내부 AES 유틸이 아니라 외부 암복호화 서비스 API 사용
- 암복호화 API 호출 순서: 암복호화 서비스 `upload` → `status` → 필요한 경우 `encrypt` 또는 `decrypt` → 필요한 경우 암복호화 서비스 `download`
- 암복호화용 upload/download API는 MI Platform의 기존 파일 upload/download API와 별개
- 구현 범위: `FileService.upload(...)` 업로드 복호화, `FileDedicatedController.downloadFile(...)` 단건 다운로드 암호화, `FileService.downloadFiles(...)` ZIP 내부 대상 엔트리 암호화
- 구현 제외: `directDownloadFiles(...)` URL 직접 다운로드
- 1차 구현 이후 점검/적용: `FileService`를 거치지 않고 직접 Excel 파일을 생성해 응답하는 API
- endpoint/인증 관리: 환경변수/시크릿으로 profile별 주입
- 저장소: 평문 유지 (전송 구간만 암호화)
- 다운로드 요청 검증: SKGAS도 기존 공통 File API와 동일하게 DB 저장값 기준의 `fileNm` + `fileSize` 검증을 유지
- 암호문 산출물: MI Platform Object Storage에 임시 업로드하지 않고 암복호화 서비스 책임으로 관리
- ZIP 다운로드 암호화 단위: ZIP 전체가 아니라 ZIP 내부 대상 Office 엔트리 단위

## 7. 후속 보류 항목

### 직접 Excel 생성 다운로드 암호화

일부 API는 `FileService.downloadFile(...)`을 호출하지 않고 서비스/컨트롤러에서 Excel byte stream을 직접 생성해 `ResponseEntity<byte[]>`, `ByteArrayResource`, `InputStreamResource` 등으로 응답한다. 이 경로는 단건 파일 다운로드 훅으로 자동 보호되지 않는다.

1차 단건 업로드/단건 다운로드 훅 구현 후, 직접 Excel 생성 API를 별도로 audit하고 SKGAS 대상이면 응답 직전에 암호화 API를 적용한다.

현재 검색으로 확인된 우선 점검 후보:

- `src/main/java/com/sk/domain/admin/controller/ExcelController.java`
- `src/main/java/com/sk/domain/mi/controller/MiForecastDedicatedController.java`
- `src/main/java/com/sk/domain/mi/controller/MiForecastController.java`
- `src/main/java/com/sk/domain/mi/controller/MiDashboardController.java`
- `src/main/java/com/sk/domain/mi/controller/MiDashboardDedicatedController.java`
- `src/main/java/com/sk/domain/mi/controller/nextgen/ScenarioGroupController.java`
- `src/main/java/com/sk/domain/mi/service/MiForecastService.java`
- `src/main/java/com/sk/domain/mi/service/MiDashboardService.java`
- `src/main/java/com/sk/domain/mi/service/MiDataDetailService.java`
- `src/main/java/com/sk/domain/mi/service/nextgen/ScenarioGroupServiceImpl.java`

후속 적용 시에는 공통 `encryptGeneratedOfficeFileIfNeeded(...)` 형태의 작은 서비스 메서드로 중복을 줄이고, 각 API가 요청자 고객사 식별을 안정적으로 할 수 있는지 먼저 확인한다.

### 직접 URL 다운로드 암복호화

`directDownloadFiles()`는 파일 URL 목록을 기준으로 단건 또는 ZIP 응답을 만든다. 이 경로는 `FileDownloadReq` 기반의 `fileNo`/`fileNm`/`fileSize` 검증을 거치지 않으므로 별도 설계 후 적용한다.

후속 구현 시에는 아래 정책을 별도로 결정한다.

- URL 직접 다운로드 경로에서 요청자 고객사 식별을 어떻게 보장할지
- URL만으로 원본 파일명과 확장자를 신뢰해도 되는지, 또는 별도 metadata 조회가 필요한지
- 단건 URL 다운로드와 URL 기반 ZIP 다운로드 모두 같은 엔트리 단위 암호화 정책을 적용할지

## 8. Implementation Readiness

- readiness: `ready after minor cleanup`
- ZIP 다운로드 암호화 정책은 구현 가능한 수준으로 확정되었다.
- 구현 전 운영값 확인 필요: 최대 파일 크기, ZIP당 최대 파일 수, 외부 암복호화 서비스 동시 호출 제한.
- 구현 시 검증 필요: SKGAS + Office ZIP 엔트리는 암호화되고, 비대상 엔트리는 기존과 동일하게 포함되며, `fileNm`/`fileSize` 검증은 평문 DB metadata 기준으로 유지된다.
