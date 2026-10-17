package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.mi.dto.request.nextgen.generation.EventGenerationReq;
import com.sk.domain.mi.dto.response.nextgen.generation.EventGenerationRes;
import com.sk.domain.shared.client.EventGenerationAgentApi;
import com.sk.domain.shared.client.dto.response.EventGenerationAgentRes;
import com.sk.domain.shared.config.EventGenerationJobProperties;
import com.sk.domain.shared.entity.master.tmd.MiEventGenerationJob;
import com.sk.domain.shared.repository.master.tmd.MiEventGenerationJobRepository;
import jakarta.annotation.PreDestroy;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * [MCP Event Generate] 이벤트 생성 작업 실행기. 전용 실행기 스레드에서 이벤트 생성 서버를 동기 호출하고(최대 15분) 결과를 작업 행에 남긴다.
 *
 * 이 인스턴스가 맡은 작업 ID 를 들고 있으며, 이 수가 서버당 동시 생성 한도의 기준이다 (서버가 늘면 전체 허용 수도 는다).
 * 맡은 작업은 heartbeat 로 살아 있음을 알리고, heartbeat 가 끊긴 RUNNING 작업(다른 인스턴스 포함)은 INTERRUPTED 로 정리한다.
 * 종료될 때는 맡은 작업을 바로 INTERRUPTED 로 바꿔 stale-after 만큼 RUNNING 으로 남지 않게 한다.
 */
@Slf4j
@Profile("dedicated")
@Component
@RequiredArgsConstructor
public class EventGenerationJobRunner {

    static final String INTERRUPTED_MESSAGE =
        "이벤트 생성 결과를 받지 못했습니다(서버 재기동 등). 생성 서버가 이벤트를 저장했을 수 있으니 다시 생성하기 전에 이벤트 목록에서 확인하세요.";

    private final EventGenerationAgentApi eventGenerationAgentApi;
    private final MiEventGenerationJobRepository jobRepository;
    private final EventGenerationJobProperties properties;
    private final ObjectMapper objectMapper;

    /** 이 인스턴스가 맡은 작업 ID (접수~종료). 서버당 한도와 heartbeat 의 기준. */
    private final Set<Long> runningJobIds = ConcurrentHashMap.newKeySet();

    /** 종료 중이면 통신 실패를 FAILED 로 덮어쓰지 않는다 (종료 시 이미 INTERRUPTED 로 바꿨다). */
    private volatile boolean shuttingDown;

    /** 이 인스턴스가 맡은 작업 수. */
    public int runningCount() {
        return runningJobIds.size();
    }

    /** 실행기에 넘기기 전에 작업을 이 인스턴스 몫으로 잡는다 (한도 확인과 같은 잠금 안에서 호출). */
    public void reserve(Long jobId) {
        runningJobIds.add(jobId);
    }

    public void run(Long jobId, String userLoginId, EventGenerationReq request) {
        try {
            EventGenerationRes result = callAgent(userLoginId, request);
            String payload = toJson(result);
            update(jobId, job -> job.succeed(payload, LocalDateTime.now()));
        } catch (AppErrorException e) {
            fail(jobId, e.getErrorCode(), e.getAdditionalMessage() != null ? e.getAdditionalMessage() : e.getMessage());
        } catch (ApiErrorException e) {
            if (e.getErrorCode() != null) {
                fail(jobId, e.getErrorCode(), e.getMessage());
            } else {
                // 공통 RestClient 빌더가 HTTP 오류 status 를 statusCode 로 담아 던진 경우
                fail(jobId, ResultCode.Error.API_RESPONSE_ERROR, "HTTP %d: %s".formatted(e.getStatusCode(), e.getMessage()));
            }
        } catch (RuntimeException e) {
            log.error("[Event Generate Job] jobId={} 처리 중 예상하지 못한 오류", jobId, e);
            fail(jobId, ResultCode.Error.SYS_INTERNAL_ERROR, e.getMessage());
        } finally {
            runningJobIds.remove(jobId);
        }
    }

    /** 실행기에 넣지 못한 작업(스레드 포화)을 실패로 닫는다. */
    public void failRejected(Long jobId) {
        runningJobIds.remove(jobId);
        fail(jobId, ResultCode.Error.BIZ_RULE_VIOLATION, "동시 생성 한도를 넘어 실행하지 못했습니다.");
    }

    /**
     * 실행 중 작업 heartbeat 갱신 + heartbeat 가 끊긴 작업 정리. 주기는 mcp.event-generation.heartbeat-rate-ms (기본 60초).
     */
    @Scheduled(fixedRateString = "${mcp.event-generation.heartbeat-rate-ms:60000}")
    public void heartbeat() {
        LocalDateTime now = LocalDateTime.now();
        try {
            List<Long> ids = List.copyOf(runningJobIds);
            if (!ids.isEmpty()) {
                jobRepository.touchHeartbeat(ids, now);
            }
            int interrupted = jobRepository.interruptStale(
                now.minus(properties.getStaleAfter()), now, ResultCode.Error.SYS_INTERNAL_ERROR.getCode(), INTERRUPTED_MESSAGE);
            if (interrupted > 0) {
                log.warn("[Event Generate Job] heartbeat 가 끊긴 작업 {}건을 INTERRUPTED 로 정리했습니다.", interrupted);
            }
        } catch (RuntimeException e) {
            log.error("[Event Generate Job] heartbeat 처리 실패", e);
        }
    }

    /**
     * 종료 시 이 인스턴스가 맡은 RUNNING 작업을 바로 INTERRUPTED 로 바꾼다. 실행 스레드는 생성 서버 응답을 더 기다리지 못한다.
     * 종료 전에 결과가 도착하면 run() 이 실제 결과로 덮어쓴다.
     */
    @PreDestroy
    public void interruptOwnJobsOnShutdown() {
        shuttingDown = true;
        List<Long> ids = List.copyOf(runningJobIds);
        if (ids.isEmpty()) {
            return;
        }
        try {
            int interrupted = jobRepository.interruptRunning(
                ids, LocalDateTime.now(), ResultCode.Error.SYS_INTERNAL_ERROR.getCode(), INTERRUPTED_MESSAGE);
            log.warn("[Event Generate Job] 종료로 실행 중 작업 {}건을 INTERRUPTED 로 바꿨습니다. jobIds={}", interrupted, ids);
        } catch (RuntimeException e) {
            log.error("[Event Generate Job] 종료 시 작업 정리 실패 (stale-after 경과 후 다른 인스턴스가 정리). jobIds={}", ids, e);
        }
    }

    /**
     * 생성 서버 호출. 통신 예외(호스트 다운/타임아웃/역직렬화 실패)를 통제된 외부 API 에러로 변환한다.
     * HTTP 오류 status 는 공통 RestClient 빌더가 이미 ApiErrorException 으로 던진다 (PriceInterpretationServiceImpl 과 같은 방식).
     */
    private EventGenerationRes callAgent(String userLoginId, EventGenerationReq request) {
        EventGenerationAgentRes res;
        try {
            res = eventGenerationAgentApi.generate(userLoginId, request);
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[Event Generate Agent] 통신 오류: {}", e.getMessage(), e);
            throw ApiErrorException.of(code, e);
        } catch (RestClientException e) {
            log.error("[Event Generate Agent] 호출 실패: {}", e.getMessage(), e);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        }

        if (res == null || !res.isSuccess()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION,
                "event generation failed: %s".formatted(res == null ? "empty response" : res.message()));
        }
        if (res.data() == null) {
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "event generation succeeded without data");
        }
        return res.data();
    }

    private void fail(Long jobId, ResultCode.Error code, String message) {
        if (shuttingDown) {
            log.warn("[Event Generate Job] jobId={} 종료 중 실패({}: {}) — INTERRUPTED 로 둔다", jobId, code.getCode(), message);
            return;
        }
        update(jobId, job -> job.fail(code.getCode(), message, LocalDateTime.now()));
    }

    private void update(Long jobId, Consumer<MiEventGenerationJob> change) {
        try {
            jobRepository.findById(jobId).ifPresentOrElse(job -> {
                change.accept(job);
                jobRepository.save(job);
            }, () -> log.warn("[Event Generate Job] jobId={} 작업 행이 없어 결과를 남기지 못했습니다.", jobId));
        } catch (RuntimeException e) {
            // 종료 중 DB 연결이 닫힌 경우 등. 작업은 INTERRUPTED(종료 시) 또는 stale 정리로 닫힌다
            log.error("[Event Generate Job] jobId={} 결과 기록 실패", jobId, e);
        }
    }

    private String toJson(EventGenerationRes result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw AppErrorException.of(ResultCode.Error.SYS_INTERNAL_ERROR, "event generation result serialization failed");
        }
    }

}
