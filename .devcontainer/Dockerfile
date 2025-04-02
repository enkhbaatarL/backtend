# 베이스 이미지: OpenJDK 17
FROM gradle:8-jdk17

# 시간대 설정 (선택 사항)
ENV TZ=Asia/Seoul

# 작업 디렉토리 생성
WORKDIR /workspace

# 프로젝트 파일 복사
COPY . .

# 포트 설정
EXPOSE 8080

# 기본 명령어
# CMD ["gradle", "--version"]
ENTRYPOINT ["chmod", "+x", "./gradlew"]