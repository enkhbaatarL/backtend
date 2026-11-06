package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.EventVerificationUpdateReq;
import com.sk.domain.mi.dto.response.nextgen.EventVerificationUpdateRes;
import com.sk.domain.shared.client.EventVerificationUpdateAgentApi;
import com.sk.domain.shared.client.dto.request.EventVerificationUpdateAgentReq;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.validation.Validator;
import java.net.SocketTimeoutException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
public class EventVerificationUpdateService {
    private static final Set<String> FIELDS = Set.of("one_line_summary", "search_text", "key_bullets_json",
        "detail_analysis", "numeric_facts_md", "institution_view_synthesis");
    private static final String CHECK_SAVED = " 자동 재시도하지 말고 Event 상세와 Snapshot 저장 여부를 먼저 확인해 주세요.";
    private final EventVerificationUpdateAgentApi agentApi;
    private final EventVerificationAccessService accessService;
    private final AuthorizationService authorizationService;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    // 긴 Agent 호출 동안 DB 트랜잭션을 유지하지 않는다. Snapshot/Event 쓰기는 Agent가 소유한다.
    public EventVerificationUpdateRes update(Long eventId, EventVerificationUpdateReq request) {
        var jwt = authorizationService.getCurrentJwt();
        // client_credentials 토큰의 subject는 사용자 ID가 아닌 client ID다.
        if (jwt.getClaim(Claim.USER_NO.getCode()) == null) {
            throw error(HttpStatus.FORBIDDEN, ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "로그인 사용자 토큰으로 요청해 주세요.");
        }
        String actor = jwt.getSubject();
        if (actor == null || actor.isBlank() || actor.length() > 100) {
            throw error(HttpStatus.UNAUTHORIZED, ResultCode.Error.AUTH_INVALID_CREDENTIALS, "로그인 사용자 ID를 확인할 수 없습니다.");
        }
        accessService.checkAccessible(List.of(eventId));
        EventVerificationUpdateRes response;
        try {
            response = agentApi.update(eventId, new EventVerificationUpdateAgentReq(actor, request.expectedUpdatedAt()));
        } catch (RestClientResponseException ex) {
            String errorName = errorName(ex);
            log.warn("Event verification update Agent error: eventId={}, status={}, errorName={}", eventId, ex.getStatusCode().value(), errorName);
            throw switch (ex.getStatusCode().value()) {
                case 404 -> error(HttpStatus.NOT_FOUND, ResultCode.Error.RSC_NOT_FOUND, "Event가 없거나 삭제·미게시 상태입니다.");
                case 409 -> error(HttpStatus.CONFLICT, ResultCode.Error.BIZ_EVENT_UPDATE_STALE, "최신 Event를 다시 조회한 후 요청해 주세요.");
                case 422 -> error(HttpStatus.UNPROCESSABLE_ENTITY, ResultCode.Error.REQ_INVALID_PARAMETER, "Event 검증 업데이트 요청값을 확인해 주세요.");
                case 500 -> "EVENT_UPDATE_SCHEMA_ERROR".equals(errorName)
                    ? error(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.Error.API_EVENT_UPDATE_SCHEMA_ERROR, "EVENT_UPDATE_SCHEMA_ERROR")
                    : error(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.Error.API_RESPONSE_ERROR, "Event 검증 업데이트 처리에 실패했습니다." + CHECK_SAVED);
                case 503 -> error(HttpStatus.SERVICE_UNAVAILABLE, ResultCode.Error.API_SERVICE_UNAVAILABLE, "Event 검증 서비스를 사용할 수 없습니다." + CHECK_SAVED);
                case 504 -> timeout();
                default -> invalidResponse();
            };
        } catch (ResourceAccessException ex) {
            log.warn("Event verification update transport error: eventId={}", eventId);
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) throw timeout();
            }
            throw error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_SERVICE_UNAVAILABLE, "Agent 연결 또는 응답 수신에 실패했습니다." + CHECK_SAVED);
        } catch (RestClientException ex) {
            log.warn("Invalid Event verification update response: eventId={}", eventId);
            throw invalidResponse();
        }
        validateResponse(eventId, response);
        log.info("Event verification update completed: eventId={}, updateStatus={}, snapshotId={}",
            eventId, response.updateStatus(), response.miEventSnapshotId());
        return response;
    }

    private void validateResponse(Long eventId, EventVerificationUpdateRes response) {
        if (response == null || !validator.validate(response).isEmpty() || !eventId.equals(response.miEventId())
            || !eventId.equals(response.verification().miEventId())
            || (response.postVerification() != null && !eventId.equals(response.postVerification().miEventId()))) throw invalidResponse();
        try {
            if (!OffsetDateTime.parse(response.checkedAt()).getOffset().equals(ZoneOffset.UTC)) throw invalidResponse();
        } catch (DateTimeParseException ex) {
            throw invalidResponse();
        }
        var fields = new HashSet<>(response.changedFields());
        var changeFields = response.changes().stream().map(EventVerificationUpdateRes.Change::field).toList();
        if (!FIELDS.containsAll(fields) || fields.size() != response.changedFields().size()
            || changeFields.size() != fields.size() || !fields.equals(new HashSet<>(changeFields))) throw invalidResponse();
        if ("updated".equals(response.updateStatus())) {
            if (!"fail".equals(response.verification().status()) || response.postVerification() == null
                || !"pass".equals(response.postVerification().status()) || response.miEventSnapshotId() == null
                || response.snapshotVersionNo() == null || response.updatedAt() == null || fields.isEmpty()
                || response.changes().stream().anyMatch(c -> Objects.equals(c.before(), c.after()) || c.sourceUrls().isEmpty())) {
                throw invalidResponse();
            }
        } else {
            // Agent의 저장 직전 비교에서 실변경이 없으면 FAIL 재검증 후에도 unchanged일 수 있다.
            if (!fields.isEmpty() || response.miEventSnapshotId() != null || response.snapshotVersionNo() != null
                || response.previousMiEventSnapshotId() != null) {
                throw invalidResponse();
            }
        }
    }

    private String errorName(RestClientResponseException ex) {
        try {
            var body = objectMapper.readTree(ex.getResponseBodyAsString());
            String name = body == null ? "" : body.path("data").path("error_name").asText("");
            return name.matches("[A-Z_]{1,80}") ? name : "UNKNOWN";
        } catch (JsonProcessingException exIgnored) {
            return "UNKNOWN";
        }
    }

    private static AppHttpStatusException timeout() {
        return error(HttpStatus.GATEWAY_TIMEOUT, ResultCode.Error.API_TIMEOUT, "Event 검증 업데이트 시간이 초과되었습니다." + CHECK_SAVED);
    }

    private static AppHttpStatusException invalidResponse() {
        return error(HttpStatus.BAD_GATEWAY, ResultCode.Error.API_RESPONSE_ERROR, "Agent 업데이트 응답을 처리할 수 없습니다." + CHECK_SAVED);
    }

    private static AppHttpStatusException error(HttpStatus status, ResultCode.Error code, String message) {
        return AppHttpStatusException.of(status, code, message);
    }
}
