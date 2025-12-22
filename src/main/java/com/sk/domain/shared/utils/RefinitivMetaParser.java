package com.sk.domain.shared.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Refinitiv News Meta JSON 파싱 유틸리티
 *
 * <p>JSON 메타 정보에서 필요한 데이터를 추출합니다.</p>
 */
@Slf4j
public class RefinitivMetaParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String INFO_SOURCE_FIELD = "infoSource";
    private static final String ROLE_FIELD = "_role";
    private static final String QCODE_FIELD = "_qcode";
    private static final String SOURCE_ROLE = "sRole:source";

    /**
     * JSON 메타에서 source code를 추출합니다.
     *
     * <p>infoSource 배열에서 _role이 "sRole:source"인 항목의 _qcode 값을 반환합니다.</p>
     *
     * @param metaJson JSON 형식의 메타 정보
     * @return source code (예: "NS:RTRS"), 없으면 null
     *
     * @example
     * <pre>
     * String json = "{\"infoSource\": [{\"_role\": \"sRole:source\", \"_qcode\": \"NS:RTRS\"}]}";
     * String sourceCode = RefinitivMetaParser.extractSourceCode(json);
     * // Returns: "NS:RTRS"
     * </pre>
     */
    public static String extractSourceCode(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metaJson);
            JsonNode infoSourceArray = root.get(INFO_SOURCE_FIELD);

            if (infoSourceArray == null || !infoSourceArray.isArray()) {
                log.warn("infoSource field not found or not an array in meta JSON");
                return null;
            }

            // infoSource 배열에서 _role이 "sRole:source"인 항목 찾기
            for (JsonNode item : infoSourceArray) {
                JsonNode roleNode = item.get(ROLE_FIELD);
                if (roleNode != null && SOURCE_ROLE.equals(roleNode.asText())) {
                    JsonNode qcodeNode = item.get(QCODE_FIELD);
                    if (qcodeNode != null) {
                        String qcode = qcodeNode.asText();
                        // "NS:" 접두사 제거하여 실제 소스 코드만 반환
                        return qcode.startsWith("NS:") ? qcode.substring(3) : qcode;
                    }
                }
            }

            log.debug("No source code found with role 'sRole:source' in meta JSON");
            return null;

        } catch (Exception e) {
            log.error("Failed to parse meta JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * JSON 메타에서 전체 _qcode (접두사 포함)를 추출합니다.
     *
     * @param metaJson JSON 형식의 메타 정보
     * @return full qcode (예: "NS:RTRS"), 없으면 null
     */
    public static String extractFullQCode(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(metaJson);
            JsonNode infoSourceArray = root.get(INFO_SOURCE_FIELD);

            if (infoSourceArray == null || !infoSourceArray.isArray()) {
                return null;
            }

            for (JsonNode item : infoSourceArray) {
                JsonNode roleNode = item.get(ROLE_FIELD);
                if (roleNode != null && SOURCE_ROLE.equals(roleNode.asText())) {
                    JsonNode qcodeNode = item.get(QCODE_FIELD);
                    if (qcodeNode != null) {
                        return qcodeNode.asText();
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to parse meta JSON for full qcode: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * JSON 유효성을 검증합니다.
     *
     * @param metaJson 검증할 JSON 문자열
     * @return 유효한 JSON이면 true
     */
    public static boolean isValidJson(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return false;
        }

        try {
            objectMapper.readTree(metaJson);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
