package com.sk.common.config.security.mcp;

/**
 * [MCP 인증] MCP 인증 시크릿의 저장용 값. 평문 시크릿은 포함하지 않습니다.
 *
 * @param hashValue    PBKDF2 결과 (Base64)
 * @param saltValue    키별 랜덤 솔트 (Base64)
 * @param algorithmNm  해시 알고리즘명
 * @param iterationCnt 반복 횟수
 * @param prefixValue  화면 식별용 시크릿 앞자리
 */
public record McpSecretHash(
    String hashValue,
    String saltValue,
    String algorithmNm,
    Integer iterationCnt,
    String prefixValue
) {
}
