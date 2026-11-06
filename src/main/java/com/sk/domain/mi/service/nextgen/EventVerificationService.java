package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.EventVerificationReq;
import com.sk.domain.mi.dto.response.nextgen.EventVerificationRes;
import com.sk.domain.shared.client.EventVerificationAgentApi;
import com.sk.domain.shared.client.dto.request.EventVerificationAgentReq;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.validation.Validator;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventVerificationService {
    private static final Set<String> INCOMPLETE = Set.of("no_evidence", "search_error", "verification_error");
    private final EventVerificationAgentApi agentApi;
    private final EventVerificationAccessService accessService;
    private final Validator validator;
    private final AuthorizationService authorizationService;

    public EventVerificationRes verify(EventVerificationReq request) {
        String actor = EventVerificationActor.require(authorizationService);
        accessService.checkAccessible(request.miEventIds());
        EventVerificationRes response;
        try {
            response = agentApi.verify(new EventVerificationAgentReq(request.miEventIds(), actor));
        } catch (RestClientResponseException ex) {
            log.warn("Event verification Agent HTTP error: {}", ex.getStatusCode().value());
            // 도메인 오류/422 detail/HTML 등 서로 다른 오류 본문은 상태 코드로 처리한다.
            throw switch (ex.getStatusCode().value()) {
                case 404 -> error(HttpStatus.NOT_FOUND, ResultCode.Error.RSC_NOT_FOUND,
                    "선택한 Event가 없거나 삭제·미게시 상태입니다. 목록을 새로고침해 주세요.");
                case 422 -> error(HttpStatus.UNPROCESSABLE_ENTITY, ResultCode.Error.REQ_INVALID_PARAMETER,
                    "Event 검증 요청값을 확인해 주세요.");
                case 500 -> error(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.Error.API_RESPONSE_ERROR,
                    "Event 검증 요청을 처리하지 못했습니다.");
                case 503 -> error(HttpStatus.SERVICE_UNAVAILABLE, ResultCode.Error.API_SERVICE_UNAVAILABLE,
                    "Event 검증 서비스를 사용할 수 없습니다.");
                case 504 -> timeout();
                default -> invalidResponse();
            };
        } catch (ResourceAccessException ex) {
            log.warn("Event verification Agent transport error", ex);
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
                    throw timeout();
                }
            }
            throw error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_SERVICE_UNAVAILABLE,
                "Event 검증 Agent에 연결할 수 없습니다.");
        } catch (RestClientException ex) {
            log.warn("Invalid Event verification Agent response", ex);
            throw invalidResponse();
        }
        validateResponse(request, response);
        return response;
    }

    private void validateResponse(EventVerificationReq request, EventVerificationRes response) {
        if (response == null || !validator.validate(response).isEmpty()
            || !response.items().stream().map(EventVerificationRes.Item::miEventId).toList().equals(request.miEventIds())) {
            throw invalidResponse();
        }
        try {
            if (!OffsetDateTime.parse(response.checkedAt()).getOffset().equals(ZoneOffset.UTC)) throw invalidResponse();
        } catch (DateTimeParseException ex) {
            throw invalidResponse();
        }
        if (response.items().stream().map(EventVerificationRes.Item::verificationId).distinct().count() != response.items().size()) {
            throw invalidResponse();
        }
        for (var item : response.items()) {
            if (item.verificationId() == null || item.expiresAt() == null) throw invalidResponse();
            try {
                if (!OffsetDateTime.parse(item.expiresAt()).isAfter(OffsetDateTime.parse(response.checkedAt()))) throw invalidResponse();
            } catch (DateTimeParseException ex) {
                throw invalidResponse();
            }
            String expectedStatus = switch (item.reasonCode()) {
                case "verified", "no_contradiction" -> "pass";
                case "contradicted", "outdated" -> "fail";
                default -> "warning";
            };
            long aspects = item.checks().stream().map(EventVerificationRes.Check::aspect).distinct().count();
            if (!expectedStatus.equals(item.status())
                || (INCOMPLETE.contains(item.reasonCode()) ? !item.checks().isEmpty() : aspects != 6)) {
                throw invalidResponse();
            }
        }
    }

    private static AppHttpStatusException timeout() {
        return error(HttpStatus.GATEWAY_TIMEOUT, ResultCode.Error.API_TIMEOUT, "Event 검증 응답 시간이 초과되었습니다. 다시 시도해 주세요.");
    }

    private static AppHttpStatusException invalidResponse() {
        return error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_RESPONSE_ERROR, "Event 검증 Agent의 응답을 처리할 수 없습니다.");
    }

    private static AppHttpStatusException error(HttpStatus status, ResultCode.Error code, String message) {
        return AppHttpStatusException.of(status, code, message);
    }
}
