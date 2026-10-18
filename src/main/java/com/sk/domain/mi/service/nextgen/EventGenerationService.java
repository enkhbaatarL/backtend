package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationJobRes;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;
import com.sk.domain.shared.config.EventGenerationJobConfig;
import com.sk.domain.shared.config.EventGenerationJobProperties;
import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.nextgen.EventGenerationJobStatus;
import com.sk.domain.shared.repository.master.tmd.MiEventGenerationJobRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * [MCP Event Generate] 사용자 입력 문장 기반 이벤트 생성 — 비동기 작업.
 *
 * 이벤트 생성 서버는 동기 호출이며 최대 15분 걸린다. 접수 시 작업 행(RUNNING)만 만들고 즉시 jobId 를 돌려주며,
 * 생성 서버 호출은 전용 실행기에서 EventGenerationJobRunner 가 한다. 생성·저장 로직은 생성 서버가 맡고 B.E 는 결과를 그대로 보관한다.
 * SK Gas 전용 서버(dedicated profile)에서만 쓴다.
 */
@Slf4j
@Profile("dedicated")
@Service
public class EventGenerationService {

    private final MiEventGenerationJobRepository jobRepository;
    private final EventGenerationJobRunner jobRunner;
    private final AsyncTaskExecutor jobExecutor;
    private final EventGenerationJobProperties properties;
    private final ObjectMapper objectMapper;

    /** 한도 확인(이 서버가 맡은 작업 수)과 작업 행 생성·예약을 서버 안에서 원자적으로 묶는다. 한도는 서버당이다. */
    private final Object acceptLock = new Object();

    public EventGenerationService(MiEventGenerationJobRepository jobRepository,
                                  EventGenerationJobRunner jobRunner,
                                  @Qualifier(EventGenerationJobConfig.EXECUTOR_BEAN_NAME) AsyncTaskExecutor jobExecutor,
                                  EventGenerationJobProperties properties,
                                  ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
        this.jobExecutor = jobExecutor;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 생성 접수. 같은 사용자가 같은 입력으로 진행 중인 작업이 있으면 새로 만들지 않고 그 작업을 돌려준다 (중복 이벤트 생성 방지).
     * 이 서버가 맡은 작업이 동시 생성 한도(서버당) 이상이면 즉시 거절한다.
     */
    public EventGenerationAcceptRes accept(User user, EventGenerationReq request) {
        MiEventGenerationJob job;
        synchronized (acceptLock) {
            Optional<MiEventGenerationJob> sameRunning = jobRepository
                .findByUserNoAndStatusAndIsDeletedFalse(user.getUserNo(), EventGenerationJobStatus.RUNNING).stream()
                .filter(running -> running.hasSameInput(request.userQuery(), request.dateFrom(), request.dateTo()))
                .findFirst();
            if (sameRunning.isPresent()) {
                return EventGenerationAcceptRes.of(sameRunning.get(), true);
            }

            if (jobRunner.runningCount() >= properties.getMaxConcurrent()) {
                throw limitExceeded();
            }

            job = jobRepository.save(MiEventGenerationJob.start(
                user.getUserNo(), request.userQuery(), request.dateFrom(), request.dateTo(), LocalDateTime.now()));
            jobRunner.reserve(job.getEventGenerationJobId());
        }

        Long jobId = job.getEventGenerationJobId();
        String userLoginId = user.getUserLoginId();
        try {
            jobExecutor.execute(() -> jobRunner.run(jobId, userLoginId, request));
        } catch (TaskRejectedException e) {
            log.warn("[Event Generate Job] jobId={} 실행기 포화로 거절", jobId);
            jobRunner.failRejected(jobId);
            throw limitExceeded();
        }
        return EventGenerationAcceptRes.of(job, false);
    }

    /** 요청 사용자 본인의 작업 상태·결과. 다른 사용자 작업은 없는 작업으로 응답한다. */
    public EventGenerationJobRes getJob(User user, Long jobId) {
        MiEventGenerationJob job = jobRepository.findByEventGenerationJobIdAndUserNoAndIsDeletedFalse(jobId, user.getUserNo())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "이벤트 생성 작업을 찾을 수 없습니다."));
        EventGenerationRes result = job.getStatus() == EventGenerationJobStatus.SUCCEEDED ? fromJson(job.getResultPayload()) : null;
        return EventGenerationJobRes.of(job, result);
    }

    /** 요청 사용자 본인의 작업 목록 (최신순, result 제외). */
    public PaginationRes<EventGenerationJobRes> getJobs(User user, int page, int size) {
        return PaginationRes.toRes(jobRepository
            .findByUserNoAndIsDeletedFalseOrderByEventGenerationJobIdDesc(user.getUserNo(), PageRequest.of(page - 1, size))
            .map(job -> EventGenerationJobRes.of(job, null)));
    }

    private AppErrorException limitExceeded() {
        return AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
            "이벤트 생성은 동시에 최대 %d건까지 실행됩니다. 진행 중인 생성이 끝난 뒤 다시 요청하세요.".formatted(properties.getMaxConcurrent()));
    }

    private EventGenerationRes fromJson(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, EventGenerationRes.class);
        } catch (JsonProcessingException e) {
            log.error("[Event Generate Job] 저장된 결과 역직렬화 실패", e);
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "저장된 이벤트 생성 결과를 읽지 못했습니다.");
        }
    }

}
