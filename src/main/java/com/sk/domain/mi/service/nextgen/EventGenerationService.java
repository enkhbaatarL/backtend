package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;
import com.sk.domain.shared.client.EventGenerationAgentApi;
import com.sk.domain.shared.client.dto.response.EventGenerationAgentRes;
import com.sk.domain.shared.entity.master.tmd.User;
import java.net.SocketTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * [MCP Event Generate] 사용자 입력 문장 기반 이벤트 생성.
 * 생성·저장 로직은 이벤트 생성 서버가 맡고, B.E 는 인증된 사용자 번호를 붙여 요청·응답을 그대로 전달만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventGenerationService {

    private final EventGenerationAgentApi eventGenerationAgentApi;

    public EventGenerationRes generateEvents(User user, EventGenerationReq request) {

        EventGenerationAgentRes res = callAgent(user.getUserNo(), request);

        if (!res.isSuccess()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "event generation failed: %s".formatted(res.message()));
        }
        return res.data();
    }

    /**
     * 통신 예외(호스트 다운/타임아웃/역직렬화 실패)를 통제된 외부 API 에러(500)로 변환한다.
     * HTTP 오류 status 는 공통 RestClient 빌더가 이미 ApiErrorException 으로 던진다 (PriceInterpretationServiceImpl 과 같은 방식).
     */
    private EventGenerationAgentRes callAgent(Integer userNo, EventGenerationReq request) {
        try {
            return eventGenerationAgentApi.generate(userNo, request);
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[Event Generate Agent] 통신 오류: {}", e.getMessage(), e);
            throw ApiErrorException.of(code, e);
        } catch (RestClientException e) {
            log.error("[Event Generate Agent] 호출 실패: {}", e.getMessage(), e);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        }
    }

}
