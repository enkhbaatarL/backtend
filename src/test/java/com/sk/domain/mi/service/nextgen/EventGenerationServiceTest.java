package com.sk.domain.mi.service.nextgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationJobRes;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;
import com.sk.domain.shared.config.EventGenerationJobProperties;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import com.sk.domain.shared.repository.master.tmd.MiEventGenerationJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

// [MCP Event Generate] 접수는 작업 행만 만들고 즉시 응답하며, 생성 서버 호출은 전용 실행기의 EventGenerationJobRunner 가 한다
@DisplayName("이벤트 생성 비동기 작업 서비스")
@ExtendWith(MockitoExtension.class)
class EventGenerationServiceTest {

    private static final EventGenerationReq REQUEST = new EventGenerationReq("인도 LPG 수입 정책 변화로 이벤트 생성", "2026-09-01", null);
    private static final int USER_NO = 7;

    @Mock
    private MiEventGenerationJobRepository jobRepository;

    @Mock
    private EventGenerationJobRunner jobRunner;

    @Mock
    private AsyncTaskExecutor jobExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private EventGenerationService eventGenerationService;

    private User user;

    @BeforeEach
    void setUp() {
        EventGenerationJobProperties properties = new EventGenerationJobProperties();
        properties.setMaxConcurrent(2);
        eventGenerationService = new EventGenerationService(jobRepository, jobRunner, jobExecutor, properties, objectMapper);

        user = Mockito.mock(User.class);
        Mockito.lenient().when(user.getUserNo()).thenReturn(USER_NO);
        Mockito.lenient().when(user.getUserLoginId()).thenReturn("hong.gd");
    }

    @Test
    @DisplayName("접수하면 RUNNING 작업을 만들고 jobId 를 즉시 돌려주며, 실행기에서 사용자 로그인 ID 로 생성 서버를 호출한다")
    void acceptsAndRunsInBackground() {
        given(jobRepository.findByUserNoAndStatusAndIsDeletedFalse(USER_NO, EventGenerationJobStatus.RUNNING)).willReturn(List.of());
        given(jobRunner.runningCount()).willReturn(1);
        given(jobRepository.save(any(MiEventGenerationJob.class))).willAnswer(invocation -> withId(invocation.getArgument(0), 42L));

        EventGenerationAcceptRes res = eventGenerationService.accept(user, REQUEST);

        assertThat(res.jobId()).isEqualTo(42L);
        assertThat(res.status()).isEqualTo(EventGenerationJobStatus.RUNNING);
        assertThat(res.reused()).isFalse();

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        then(jobExecutor).should().execute(task.capture());
        then(jobRunner).should().reserve(42L); // 서버당 한도에 바로 반영
        then(jobRunner).should(never()).run(any(), any(), any()); // 접수 시점에는 생성 서버를 부르지 않는다
        task.getValue().run();
        then(jobRunner).should().run(42L, "hong.gd", REQUEST);
    }

    @Test
    @DisplayName("같은 사용자가 같은 입력으로 진행 중인 작업이 있으면 새로 만들지 않고 그 작업을 돌려준다")
    void reusesRunningJobWithSameInput() {
        MiEventGenerationJob running = withId(MiEventGenerationJob.start(USER_NO, REQUEST.userQuery(), "2026-09-01", null, LocalDateTime.now()), 41L);
        given(jobRepository.findByUserNoAndStatusAndIsDeletedFalse(USER_NO, EventGenerationJobStatus.RUNNING)).willReturn(List.of(running));

        EventGenerationAcceptRes res = eventGenerationService.accept(user, REQUEST);

        assertThat(res.jobId()).isEqualTo(41L);
        assertThat(res.reused()).isTrue();
        then(jobRepository).should(never()).save(any());
        then(jobExecutor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이 서버가 맡은 작업이 동시 생성 한도(서버당) 이상이면 작업을 만들지 않고 BIZ-001 로 거절한다")
    void rejectsWhenConcurrencyLimitReached() {
        given(jobRepository.findByUserNoAndStatusAndIsDeletedFalse(USER_NO, EventGenerationJobStatus.RUNNING)).willReturn(List.of());
        given(jobRunner.runningCount()).willReturn(2);

        assertThatThrownBy(() -> eventGenerationService.accept(user, REQUEST))
            .isInstanceOfSatisfying(AppErrorException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(ResultCode.Error.BIZ_RULE_VIOLATION);
                assertThat(e.getAdditionalMessage()).contains("최대 2건");
            });
        then(jobRepository).should(never()).save(any());
        then(jobExecutor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실행기가 포화로 거절하면 만든 작업을 실패로 닫고 BIZ-001 로 거절한다")
    void failsJobWhenExecutorRejects() {
        given(jobRepository.findByUserNoAndStatusAndIsDeletedFalse(USER_NO, EventGenerationJobStatus.RUNNING)).willReturn(List.of());
        given(jobRunner.runningCount()).willReturn(0);
        given(jobRepository.save(any(MiEventGenerationJob.class))).willAnswer(invocation -> withId(invocation.getArgument(0), 43L));
        willThrow(new TaskRejectedException("full")).given(jobExecutor).execute(any(Runnable.class));

        assertThatThrownBy(() -> eventGenerationService.accept(user, REQUEST))
            .isInstanceOfSatisfying(AppErrorException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ResultCode.Error.BIZ_RULE_VIOLATION));
        then(jobRunner).should().failRejected(43L);
    }

    @Test
    @DisplayName("작업 조회는 SUCCEEDED 면 저장된 결과를 돌려주고, 본인 작업이 아니면 RSC-001 로 응답한다")
    void getsOwnJobWithResult() throws Exception {
        EventGenerationRes result = new EventGenerationRes(new EventGenerationRes.Meta("web", "최신 정책 뉴스 필요", 1, List.of()), List.of());
        MiEventGenerationJob job = withId(MiEventGenerationJob.start(USER_NO, REQUEST.userQuery(), null, null, LocalDateTime.now()), 42L);
        job.succeed(objectMapper.writeValueAsString(result), LocalDateTime.now());
        given(jobRepository.findByEventGenerationJobIdAndUserNoAndIsDeletedFalse(42L, USER_NO)).willReturn(Optional.of(job));
        given(jobRepository.findByEventGenerationJobIdAndUserNoAndIsDeletedFalse(99L, USER_NO)).willReturn(Optional.empty());

        EventGenerationJobRes res = eventGenerationService.getJob(user, 42L);

        assertThat(res.status()).isEqualTo(EventGenerationJobStatus.SUCCEEDED);
        assertThat(res.result()).isEqualTo(result);
        assertThatThrownBy(() -> eventGenerationService.getJob(user, 99L))
            .isInstanceOfSatisfying(AppErrorException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ResultCode.Error.RSC_NOT_FOUND));
    }

    private static MiEventGenerationJob withId(MiEventGenerationJob job, Long id) {
        ReflectionTestUtils.setField(job, "eventGenerationJobId", id);
        return job;
    }

}
