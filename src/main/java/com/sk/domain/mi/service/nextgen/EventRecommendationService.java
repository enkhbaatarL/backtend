package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.EventRecommendationReq;
import com.sk.domain.mi.dto.response.nextgen.EventRecommendationRes;
import com.sk.domain.shared.client.EventRecommendationAgentApi;
import jakarta.validation.Validator;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRecommendationService {
    private final EventRecommendationAgentApi agentApi;
    private final Validator validator;

    // 외부 응답을 기다리는 동안 DB 트랜잭션/연결을 점유하지 않는다.
    public EventRecommendationRes recommend(EventRecommendationReq request) {
        EventRecommendationRes response;
        try {
            response = agentApi.recommend(request);
        } catch (ApiErrorException ex) {
            log.warn("Event recommendation Agent returned HTTP {}", ex.getStatusCode());
            throw switch (ex.getStatusCode()) {
                case 400 -> error(HttpStatus.BAD_REQUEST, ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Event 추천 요청의 처리 조건을 확인해 주세요.");
                case 404 -> error(HttpStatus.NOT_FOUND, ResultCode.Error.API_RESPONSE_ERROR,
                    "선택한 Event를 조회할 수 없습니다. 목록을 새로고침해 주세요.");
                case 422 -> error(HttpStatus.UNPROCESSABLE_ENTITY, ResultCode.Error.REQ_INVALID_PARAMETER,
                    "선택한 Event ID를 확인해 주세요.");
                case 500 -> error(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.Error.API_RESPONSE_ERROR,
                    "Event 추천에 실패했습니다.");
                case 504 -> timeout();
                default -> invalidResponse();
            };
        } catch (ResourceAccessException ex) {
            log.warn("Event recommendation Agent connection failed", ex);
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException
                    || cause instanceof java.net.http.HttpTimeoutException) {
                    throw timeout();
                }
            }
            throw error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_SERVICE_UNAVAILABLE,
                "Event 추천 Agent에 연결할 수 없습니다.");
        } catch (RestClientException ex) {
            log.warn("Invalid Event recommendation Agent response", ex);
            throw invalidResponse();
        }

        // 잘못된 성공 응답 역시 '추천 없음'으로 숨기지 않는다.
        if (response == null || !validator.validate(response).isEmpty()) {
            throw invalidResponse();
        }
        var ids = new HashSet<Long>();
        for (var item : response.items()) {
            if (!ids.add(item.miEventId()) || request.miEventIds().contains(item.miEventId())) {
                throw invalidResponse();
            }
        }
        return response;
    }

    private static AppHttpStatusException timeout() {
        return error(HttpStatus.GATEWAY_TIMEOUT, ResultCode.Error.API_TIMEOUT,
            "Event 추천 응답 시간이 초과되었습니다. 다시 시도해 주세요.");
    }

    private static AppHttpStatusException invalidResponse() {
        return error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_RESPONSE_ERROR,
            "Event 추천 Agent의 응답을 처리할 수 없습니다.");
    }

    private static AppHttpStatusException error(HttpStatus status, ResultCode.Error code, String message) {
        return AppHttpStatusException.of(status, code, message);
    }
}
