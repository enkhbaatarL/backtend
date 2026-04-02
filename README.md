# user-backend

### project structure
[README_STRUCTURE.md](docs%2FREADME_STRUCTURE.md)


### IntelliJ에서 Google Java Style 적용하는 방법

방법 1: IntelliJ 기본 설정에서 Google Java Style 적용

       File → Settings (Ctrl + Alt + S or Cmd + , on macOS) → Editor → Code Style → Java 로 이동합니다.
       우측 상단의 "Manage..." 버튼 클릭 후 "Import Scheme → IntelliJ IDEA code style XML" 선택.
       다운로드한 intellij-java-google-style.xml 파일을 선택 후 적용.
       "Apply" → "OK" 클릭.

방법 2: Google Java Format 플러그인 사용 (자동 정리)

    1. 플러그인 설치
       File → Settings → Plugins 로 이동.
       Marketplace 탭에서 "Google Java Format" 검색.
       "Install" 버튼 클릭 후 IntelliJ 재시작.
    2. 플러그인 활성화
      File → Settings → Tools → Google Java Format 이동.
      "Enable Google Java Format" 체크 후 "Apply" → "OK".
    3. 코드 자동 정리 적용
         단축키 (Ctrl + Alt + L or Cmd + Option + L on macOS) 로 Google Java Format을 적용할 수 있습니다.

방법 3: 추가 설정 (옵션)

    1. 자동 코드 스타일 적용 (Commit 시)
        Google Java Format이 Commit 전에 자동 적용되도록 설정하려면:

        File → Settings → Version Control → Before Commit 이동.
        "Reformat code" 체크.
        필요하면 "Optimize imports"도 체크.

    2. 코드 저장 시 자동 포맷팅
       File → Settings → Tools → Actions on Save 이동.
       "Reformat code" 체크.

# Client 토큰 발급 테스트 예시

curl -X POST "http://localhost:8080/oauth2/token" -H "Content-Type: application/x-www-form-urlencoded" -u "mi-client:secret"  -d "grant_type=client_credentials"


# GIT STATUS 에 나온 값을 가지고 temp 에 복사하는 방법
./copy_modified_to_temp.sh --dest ./temp <<'EOF'
modified:   README.md
modified:   src/main/java/com/sk/common/utils/JwtUtil.java
modified:   src/main/java/com/sk/domain/mi/controller/MiCompanyController.java
modified:   src/main/java/com/sk/domain/mi/dto/response/MiCompanyDetailRes.java
modified:   src/main/java/com/sk/domain/mi/dto/response/MiCompanyRes.java
modified:   src/main/java/com/sk/domain/mi/service/MiCompanyService.java
modified:   src/main/java/com/sk/domain/shared/controller/AuthController.java
modified:   src/main/java/com/sk/domain/shared/controller/ExternalController.java
modified:   src/main/java/com/sk/domain/shared/controller/SamlController.java
modified:   src/main/java/com/sk/domain/shared/dto/response/common/UserAccessPageRes.java
modified:   src/main/java/com/sk/domain/shared/entity/master/tmd/CustomerCompany.java
modified:   src/main/java/com/sk/domain/shared/repository/master/tmd/custom/CustomCustomerCompanyRepository.java
modified:   src/main/java/com/sk/domain/shared/repository/master/tmd/custom/CustomCustomerCompanyRepositoryImpl.java
modified:   src/main/java/com/sk/domain/shared/service/AuthService.java
modified:   src/main/java/com/sk/domain/shared/service/CommonService.java
modified:   src/main/java/com/sk/domain/shared/service/SamlService.java
modified:   src/main/java/com/sk/domain/test/TestController.java
modified:   src/main/java/com/sk/domain/test/TestService.java
EOF
