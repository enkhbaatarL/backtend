package com.sk.domain.shared.client;

import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.shared.client.dto.response.EventGenerationAgentRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * [MCP Event Generate] 이벤트 생성 서버 통신 클라이언트 (B.E → 이벤트 생성 서버, event_generate_v1).
 * 동기 호출이며 생성된 이벤트는 서버가 tmm_mi_mievent_m 에 저장한 뒤 결과를 바로 돌려준다.
 */
@HttpExchange
public interface EventGenerationAgentApi {

    /**
     * 사용자 입력 문장 기반 이벤트 생성.
     *
     * @param userLoginId B.E 가 인증한 요청 사용자 로그인 ID(usr_logn_id). 서버가 이벤트 생성자·수정자 ID 로 쓴다.
     */
    @PostExchange("/events/generations")
    EventGenerationAgentRes generate(@RequestHeader("X-User-ID") String userLoginId, @RequestBody EventGenerationReq req);

}
