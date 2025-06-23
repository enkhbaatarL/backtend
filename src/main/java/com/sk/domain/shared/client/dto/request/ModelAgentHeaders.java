package com.sk.domain.shared.client.dto.request;

import lombok.Builder;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AIP 통신용 공통 헤더 DTO (record 버전)
 */
@Builder
public record ModelAgentHeaders(
    Integer companyNo,
    Integer userNo,
    String appId,
    String chatId,
    String transactionId
) {

    public static final String H_COMPANY_NO = "aip-company";
    public static final String H_USER_NO = "aip-user";
    public static final String H_APP_ID = "aip-app-id";
    public static final String H_CHAT_ID = "aip-chat-id";
    public static final String H_TX_ID = "aip-transaction-id";

    /**
     * <헤더키, 값> Map 으로 변환
     */
    public Map<String, Object> toMap() {
        return Stream.of(
            Map.entry(H_COMPANY_NO, companyNo),
            Map.entry(H_USER_NO, userNo),
            Map.entry(H_APP_ID, appId),
            Map.entry(H_CHAT_ID, chatId),
            Map.entry(H_TX_ID, transactionId)
        ).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static ModelAgentHeaders of(Integer companyNo, Integer userNo, String uuid, Integer qnaNo) {
        return ModelAgentHeaders.builder()
            .companyNo(companyNo)
            .userNo(userNo)
            .appId("mi-backend")
            .chatId(uuid)
            .transactionId(uuid + "-" + qnaNo)
            .build();
    }

}
