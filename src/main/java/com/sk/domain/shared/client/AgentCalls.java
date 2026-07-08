package com.sk.domain.shared.client;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import java.net.SocketTimeoutException;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Agent HTTP 호출 공통 예외 변환.
 * 통신 예외(호스트 다운/타임아웃/HTTP 에러 status)를 통제된 외부 API 에러(ResultCode)로 변환한다 —
 * 예외→에러코드 매핑은 FE 에러 계약이므로 Agent 연동마다 복제하지 않고 여기 한 곳에서 관리한다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgentCalls {

    /**
     * Agent 호출을 감싸 통신 예외를 변환한다.
     *
     * @param logTag 로그 프리픽스 (예: "[Design Agent]", "[PriceInterp Agent]")
     * @param action 실패 메시지에 표기할 작업명
     */
    public static <T> T call(String logTag, String action, Supplier<T> call) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("{} {} 통신 오류: {}", logTag, action, e.getMessage(), e);
            throw AppErrorException.of(code, "Agent 통신 오류 (%s)".formatted(action));
        } catch (RestClientResponseException e) {
            log.error("{} {} 응답 오류: status={}, body={}", logTag, action, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "Agent 응답 오류 (%s)".formatted(action));
        } catch (RestClientException e) {
            log.error("{} {} 호출 실패: {}", logTag, action, e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent 호출 실패 (%s)".formatted(action));
        }
    }

}
