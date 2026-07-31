package com.sk.common.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.MDCUtil;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private static final String INTERNAL_EXCEPTION_DETAIL =
            "sensitive com.sk.internal.SecretService.process detail";
    private static final String EXPECTED_CLIENT_DETAIL = "predictDate는 yyyyMM 형식이어야 합니다.";

    private MockMvc mockMvc;
    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logAppender);
        logAppender.stop();
        MDCUtil.clear();
    }

    @Test
    @DisplayName("uncaught 500 응답은 내부 예외 메시지를 노출하지 않고 서버 로그에는 원인을 남긴다")
    void sanitizeUncaughtExceptionResponseAndKeepInternalLog() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get("/test/uncaught"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.code").value("SYS-001"))
                .andExpect(jsonPath("$.message").value("An internal server error has occurred."))
                .andReturn();

        // then
        String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(responseBody)
                .doesNotContain(INTERNAL_EXCEPTION_DETAIL)
                .doesNotContain("SecretService")
                .doesNotContain("NullPointerException");
        assertThat(MDCUtil.getValue(MDCUtil.RESPONSE_ERROR_MSG))
                .doesNotContain(INTERNAL_EXCEPTION_DETAIL);
        assertThat(logAppender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                    .contains("Unhandled Internal Error")
                    .contains(INTERNAL_EXCEPTION_DETAIL);
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getThrowableProxy().getClassName())
                    .isEqualTo(NullPointerException.class.getName());
        });
    }

    @Test
    @DisplayName("예상된 AppErrorException의 400 detail은 기존처럼 유지한다")
    void preserveExpectedClientErrorDetail() throws Exception {
        mockMvc.perform(get("/test/app-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.code").value("REQ-003"))
                .andExpect(jsonPath("$.message").value(
                        "One or more parameters are invalid.(%s)".formatted(EXPECTED_CLIENT_DETAIL)));
    }

    @RestController
    private static class ThrowingController {

        @GetMapping("/test/uncaught")
        void uncaught() {
            throw new NullPointerException(INTERNAL_EXCEPTION_DETAIL);
        }

        @GetMapping("/test/app-error")
        void appError() {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, EXPECTED_CLIENT_DETAIL);
        }
    }
}
