#!/bin/bash

# RSA 키 쌍 생성 스크립트
echo "=== JWT용 RSA 키 쌍 생성 ==="

# 키 저장 디렉토리 생성
mkdir -p keys

# 1. 개인키 생성 (PKCS#8 형식, 2048비트)
echo "1. 개인키 생성 중..."
openssl genrsa -out keys/private-key.pem 2048

# 2. 개인키를 PKCS#8 형식으로 변환
echo "2. 개인키를 PKCS#8 형식으로 변환 중..."
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keys/private-key.pem -out keys/private-key-pkcs8.pem

# 3. 공개키 추출
echo "3. 공개키 추출 중..."
openssl rsa -in keys/private-key.pem -pubout -out keys/public-key.pem

# 4. Base64 인코딩 (개행 제거)
echo "4. Base64 인코딩 중..."
echo "=== Private Key (Base64) ==="
cat keys/private-key-pkcs8.pem | grep -v "BEGIN\|END" | tr -d '\n'
echo ""
echo ""

echo "=== Public Key (Base64) ==="
cat keys/public-key.pem | grep -v "BEGIN\|END" | tr -d '\n'
echo ""
echo ""

# 5. 환경변수 형태로 출력
echo "=== 환경변수 형태 ==="
echo "export JWT_PRIVATE_KEY=\"$(cat keys/private-key-pkcs8.pem | grep -v "BEGIN\|END" | tr -d '\n')\""
echo "export JWT_PUBLIC_KEY=\"$(cat keys/public-key.pem | grep -v "BEGIN\|END" | tr -d '\n')\""

echo ""
echo "✅ 키 생성 완료!"
echo "📁 파일 위치:"
echo "  - 개인키: keys/private-key-pkcs8.pem"
echo "  - 공개키: keys/public-key.pem"
echo ""
echo "⚠️  보안 주의사항:"
echo "  - 개인키는 절대 외부에 노출하지 마세요"
echo "  - Git에 커밋하지 마세요"
echo "  - 운영환경에서는 안전한 키 관리 시스템 사용 권장"