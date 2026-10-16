package com.sk.domain.mi.service.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;
import com.sk.domain.shared.client.EventGenerationAgentApi;
import com.sk.domain.shared.client.dto.response.EventGenerationAgentRes;
import com.sk.domain.shared.entity.master.tmd.User;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

// [MCP Event Generate] B.E 는 인증된 사용자 로그인 ID 만 붙여 이벤트 생성 서버에 그대로 위임한다
@DisplayName("이벤트 생성 위임 서비스")
@ExtendWith(MockitoExtension.class)
class EventGenerationServiceTest {

    private static final EventGenerationReq REQUEST = new EventGenerationReq("인도 LPG 수입 정책 변화로 이벤트 생성", "2026-09-01", null);

    @Mock
    private EventGenerationAgentApi eventGenerationAgentApi;

    @InjectMocks
    private EventGenerationService eventGenerationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = Mockito.mock(User.class);
        given(user.getUserLoginId()).willReturn("hong.gd");
    }

    @Test
    @DisplayName("사용자 로그인 ID(usr_logn_id)를 X-User-ID 로, 요청은 그대로 넘기고 생성 서버의 data 를 그대로 돌려준다")
    void delegatesWithUserLoginIdAndReturnsData() {
        EventGenerationRes data = new EventGenerationRes(new EventGenerationRes.Meta("web", "최신 정책 뉴스 필요", 1, List.of()), List.of());
        given(eventGenerationAgentApi.generate("hong.gd", REQUEST)).willReturn(new EventGenerationAgentRes("success", "", data));

        assertThat(eventGenerationService.generateEvents(user, REQUEST)).isSameAs(data);
        then(eventGenerationAgentApi).should().generate("hong.gd", REQUEST);
    }

    @Test
    @DisplayName("생성 서버가 status=error 를 주면 서버 message 를 담아 400(BIZ-001)으로 돌려준다")
    void throwsWithAgentMessageWhenStatusIsError() {
        given(eventGenerationAgentApi.generate(anyString(), any())).willReturn(new EventGenerationAgentRes("error", "classify failed", null));

        assertThatThrownBy(() -> eventGenerationService.generateEvents(user, REQUEST))
            .isInstanceOf(AppErrorException.class)
            .satisfies(e -> {
                assertThat(((AppErrorException) e).getErrorCode()).isEqualTo(ResultCode.Error.BIZ_RULE_VIOLATION);
                assertThat(((AppErrorException) e).getAdditionalMessage()).contains("classify failed");
            });
    }

    @Test
    @DisplayName("타임아웃은 API_TIMEOUT, 연결 실패는 API_SERVICE_UNAVAILABLE 로 바꾼다")
    void mapsCommunicationErrors() {
        given(eventGenerationAgentApi.generate(anyString(), any()))
            .willThrow(new ResourceAccessException("timeout", new SocketTimeoutException()))
            .willThrow(new ResourceAccessException("refused", new ConnectException()));

        assertThatThrownBy(() -> eventGenerationService.generateEvents(user, REQUEST))
            .isInstanceOfSatisfying(ApiErrorException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ResultCode.Error.API_TIMEOUT));
        assertThatThrownBy(() -> eventGenerationService.generateEvents(user, REQUEST))
            .isInstanceOfSatisfying(ApiErrorException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ResultCode.Error.API_SERVICE_UNAVAILABLE));
    }

}
