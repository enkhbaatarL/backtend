package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AgentDesignApiException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartV4Req;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioValidationCache;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignValidationStore;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignPresentationMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignTableMapper;
import com.sk.domain.shared.client.ScenarioDesignAgentV4Api;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupOperationRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.annotation.PreDestroy;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 멀티 이벤트 시나리오 설계 (v4) 서비스.
 *
 * B.E 역할은 셋이다 — 진입 검증과 userId 주입, Agent SSE의 FE 직결 중계, presentation 가공 조회.
 * 그룹/시나리오/operation의 DB 적재는 전부 Agent가 수행하고, B.E는 화면 계약 변환과
 * DB 합성(그룹 이벤트 목록, 실패 사유)만 더한다.
 *
 * 중계 구조: 컨트롤러 스레드는 Agent 응답 status가 확정될 때까지만 대기한다(connectGate).
 * JSON 오류(중복 정책 등)면 그대로 예외로 변환해 BaseResponse 오류 응답이 되고,
 * 스트림이 열리면 SseEmitter를 반환한 뒤 전용 스레드가 프레임을 계속 중계한다.
 *
 * 재연결 정책(이중 구조): 정상 경로는 DB 선조회 — 내 진행중 작업 중 동일 이벤트 조합이 있으면
 * 생성 요청 없이 바로 해당 operation 스트림에 재연결한다. 선조회와 생성 사이의 레이스는
 * Agent 409 백스톱이 흡수한다(응답의 식별자로 같은 emitter에 재연결 — 계약 5.2의 정석 플로우).
 * 어느 경로든 이력 전체(sequence 0부터)를 replay한 뒤 live 이벤트를 이어받으므로,
 * FE는 연결 끊김/새로고침 후에도 같은 생성 API 재호출만으로 복구된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignV4ServiceImpl implements ScenarioDesignV4Service {

    /** 생성 평균 150초 + 검토/재시도 여유. FE 이탈 시 emitter 타임아웃으로 정리된다. */
    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60 * 1000;
    private static final long CONNECT_TIMEOUT_SECONDS = 10L;

    /** 실패 사유(lastError) 합성 대상 상태 — Agent operation status 어휘 기준 종료 2종. */
    private static final Set<String> FAILED_OPERATION_STATUSES = Set.of("failed", "interrupted");

    /** 설계 시작 선조회 대상 상태 — Agent operation status 어휘 기준 진행중 2종. */
    private static final List<String> ACTIVE_OPERATION_STATUSES = List.of("queued", "running");

    private final ScenarioDesignAgentV4Api agentClient;
    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupOperationRepository operationRepository;
    private final MiScenarioGroupEventRepository groupEventRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final AuthorizationService authorizationService;

    private final AtomicLong threadSequence = new AtomicLong();
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "design-v4-sse-%d".formatted(threadSequence.incrementAndGet()));
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    void shutdown() {
        streamExecutor.shutdownNow();
    }

    /**
     * [설계 시작 - v4] 이벤트 1~5개 검증 후 Agent 생성 스트림을 개설해 FE로 직결 중계한다.
     * 개수/중복/양수는 B.E 선검증으로 Agent의 FastAPI 422 응답이 FE로 새는 것을 막는다.
     * 이벤트 점유 선검사와 Redis 락은 두지 않는다 — 중복 생성 방지는 Agent의 event_set_hash 정책이 담당한다.
     *
     * 재연결은 이중 구조다. 정상 경로는 DB 선조회 — 내 진행중 작업 중 동일 이벤트 조합이 있으면
     * 생성 요청 없이 바로 그 operation 스트림에 재연결한다(0부터 replay — 이벤트 수가 적어 부담 없고,
     * FE는 sequence 중복만 무시하면 된다). 선조회와 생성 사이의 레이스는 백스톱이 흡수한다 —
     * Agent가 409를 주면 응답의 식별자로 같은 방식으로 재연결한다(계약 5.2의 정석 플로우).
     * 그 외 정책 판정(400 확정 전 그룹 존재, 403 타 사용자 소유 등)은 복제하지 않고 Agent 응답을 그대로 내린다.
     */
    @Override
    public SseEmitter startDesignStream(ScenarioDesignStartV4Req request) {
        validateEventIds(request.miEventIds());
        Long userId = authorizationService.getUserNo().longValue();

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);

        ReconnectTarget active = findMyActiveOperation(userId, request.miEventIds());

        if (active != null) {
            log.info("[Design V4] 내 진행중 동일 조합 작업 선조회 적중 - 재연결: scenarioGroupId={}, operationId={}", active.scenarioGroupId(), active.operationId());
            openStreamOn(emitter,
                (target, connectGate) -> agentClient.pipeOperationStream(
                    active.scenarioGroupId(), active.operationId(), null, target, connectGate),
                "resumeOperationStream");
            return emitter;
        }

        ScenarioGroupCreateV4AgentReq agentRequest = ScenarioGroupCreateV4AgentReq.of(request.miEventIds(), userId);

        try {
            openStreamOn(emitter,
                (target, connectGate) -> agentClient.pipeCreateStream(agentRequest, target, connectGate),
                "startDesignStream");
        } catch (AgentDesignApiException e) {
            ReconnectTarget reconnect = resolveReconnectTarget(e);
            if (reconnect == null) {
                throw e;
            }
            log.info("[Design V4] 동일 조합 작업 진행 중 - 자동 재연결: scenarioGroupId={}, operationId={}", reconnect.scenarioGroupId(), reconnect.operationId());
            openStreamOn(emitter,
                (target, connectGate) -> agentClient.pipeOperationStream(
                    reconnect.scenarioGroupId(), reconnect.operationId(), null, target, connectGate),
                "reconnectOperationStream");
        }
        return emitter;
    }

    /**
     * [설계 화면 데이터 조회] Agent presentation을 B.E 계약으로 가공해 내린다 — 설계 화면의 단일 조회 API.
     * Agent 응답에 없는 화면 재료를 B.E에서 합성한다: 그룹 구성 이벤트 목록(매핑 테이블),
     * operation 실패 사유(lastError, 종료 상태일 때만), 설계 세션 상태(designSession — 그룹 엔티티 + Redis 검토 캐시).
     * 검토를 통과한 편집본(DB 미반영)이 캐시에 있으면 table을 편집본으로 오버레이하므로,
     * 검토 후 재조회도 이 API 하나로 충분하다 (세션 상태 조회 API의 역할 흡수 — 2026-08-14 단일화 결정).
     */
    @Override
    public ScenarioDesignPresentationRes getPresentation(Long scenarioGroupId, String operationId) {
        JsonNode agentBody = agentClient.getPresentation(scenarioGroupId, operationId);
        List<ScenarioGroupBaseEventRow> baseEvents = scenarioGroupRepository.findBaseEventsByScenarioGroupId(scenarioGroupId);

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId).orElse(null);
        ScenarioValidationCache cache = validationStore.find(scenarioGroupId).orElse(null);
        DesignTable overlayTable = cache == null ? null : ScenarioDesignTableMapper.toDesignTable(cache.table());

        return ScenarioDesignPresentationMapper.toRes(
            agentBody,
            baseEvents,
            resolveLastError(agentBody),
            toDesignSession(group, cache, overlayTable != null),
            toGroupMeta(group),
            overlayTable,
            loadSignpostsByScenario(group));
    }

    /** 설계 세션 상태 합성 — 단계/점유는 그룹 엔티티, 검토 결과는 Redis 캐시가 원천이다. */
    private ScenarioDesignPresentationRes.DesignSessionState toDesignSession(MiScenarioGroup group,
                                                                             ScenarioValidationCache cache,
                                                                             boolean editOverlayApplied) {
        if (group == null) {
            return null;
        }
        ScenarioDesignPresentationRes.DesignSessionValidation validation = cache == null
            ? null
            : new ScenarioDesignPresentationRes.DesignSessionValidation(cache.isValid(), cache.reason());
        return ScenarioDesignPresentationRes.DesignSessionState.of(
            group.getStatus(),
            group.getStage(),
            group.getStatus() == ScenarioGroupStatus.CREATING,
            group.getRequestUserId(),
            validation,
            editOverlayApplied);
    }

    /** 그룹 메타 합성 — Signpost 산출 시 Agent가 채우는 그룹명/요약/설명 (확정 화면 편집 초기값). */
    private ScenarioDesignPresentationRes.DesignGroupMeta toGroupMeta(MiScenarioGroup group) {
        if (group == null) {
            return null;
        }
        return new ScenarioDesignPresentationRes.DesignGroupMeta(group.getTitle(), group.getSummary(), group.getDescription());
    }

    /**
     * 행별 Signpost 목록 합성 (Signpost 산출 이후 단계에서만). 사용 중인 것만 표시 순으로 내린다.
     * Agent presentation에는 Signpost가 없어 DB에서 합성한다 — 확정 요청 재투영의 원천 데이터다.
     */
    private Map<Long, List<ScenarioSignpostDefinitionViewRes>> loadSignpostsByScenario(MiScenarioGroup group) {
        if (group == null
            || (group.getStage() != ScenarioGroupStage.SIGNPOST_CREATED && group.getStage() != ScenarioGroupStage.COMPLETED)) {
            return Map.of();
        }
        return scenarioGroupRepository.findSignpostEntitiesByGroupId(group.getScenarioGroupId()).stream()
            .filter(sp -> Boolean.TRUE.equals(sp.getIsUsed()))
            .sorted(Comparator.comparing(sp -> sp.getDisplayOrder() == null ? Integer.MAX_VALUE : sp.getDisplayOrder()))
            .collect(Collectors.groupingBy(
                sp -> sp.getScenario().getScenarioId(),
                Collectors.mapping(sp -> ScenarioSignpostDefinitionViewRes.of(
                    sp.getSignpostId(), sp.getLabel(), sp.getProbabilityDirection(),
                    sp.getProbabilityImpactScore() == null ? null : sp.getProbabilityImpactScore().intValue(),
                    sp.getStrength(), sp.getSignpostReason(), sp.getDisplayOrder()),
                    Collectors.toList())));
    }

    /** Agent 응답의 operation이 실패 종료 상태면 operation 테이블에서 실패 사유 원문을 읽어온다. */
    private JsonNode resolveLastError(JsonNode agentBody) {
        JsonNode operation = agentBody.path("operation");
        String operationId = operation.path("operationId").asText(null);
        String status = operation.path("status").asText(null);
        if (operationId == null || status == null || !FAILED_OPERATION_STATUSES.contains(status)) {
            return null;
        }
        String lastError = operationRepository.findById(operationId)
            .map(MiScenarioGroupOperation::getLastError)
            .orElse(null);
        if (lastError == null || lastError.isBlank()) {
            return null;
        }
        try {
            return JsonUtil.fromJson(lastError, JsonNode.class);
        } catch (Exception e) {
            log.warn("[Design V4] lastError 파싱 실패: operationId={}, {}", operationId, e.getMessage());
            return null;
        }
    }

    /**
     * 내(요청 사용자) 진행중 작업 중 요청 이벤트 조합과 정확히 일치하는 그룹의 작업을 찾는다 (선조회 정상 경로).
     * 확신 가능한 사실만 DB로 판정한다 — 소유자(그룹 request_usr_id), 진행중 상태, 이벤트 집합 일치.
     * 그 외 정책 판정(확정 전 그룹, 실패 그룹 재생성, 타 사용자 소유)은 복제하지 않고 Agent에 맡긴다.
     * 사용자의 진행중 작업은 많아야 수 건이라 그룹별 이벤트 집합 비교는 Java에서 수행한다.
     */
    private ReconnectTarget findMyActiveOperation(Long userId, List<Long> miEventIds) {
        Set<Long> requested = Set.copyOf(miEventIds);
        return operationRepository
            .findByScenarioGroup_RequestUserIdAndStatusInAndIsDeletedFalseAndScenarioGroup_IsDeletedFalseOrderByCreateDtDesc(
                userId, ACTIVE_OPERATION_STATUSES)
            .stream()
            .filter(operation -> requested.equals(Set.copyOf(
                groupEventRepository.findActiveEventIdsByScenarioGroupId(operation.getScenarioGroup().getScenarioGroupId()))))
            .findFirst()
            .map(operation -> new ReconnectTarget(operation.getScenarioGroup().getScenarioGroupId(), operation.getOperationId()))
            .orElse(null);
    }

    /**
     * 409(동일 조합 작업 진행 중)이고 재연결에 필요한 두 ID가 모두 있으면 재연결 대상을 만든다.
     * Agent 계약상 409 data에는 scenarioGroupId와 operationId가 함께 오며, 타 사용자 소유면 409가 아니라 403이다.
     */
    private ReconnectTarget resolveReconnectTarget(AgentDesignApiException e) {
        if (e.getHttpStatus() != HttpStatus.CONFLICT || e.getAgentData() == null) {
            return null;
        }
        JsonNode data = e.getAgentData();
        long scenarioGroupId = data.path("scenarioGroupId").asLong(0);
        String operationId = data.path("operationId").asText(null);
        if (scenarioGroupId <= 0 || operationId == null || operationId.isBlank()) {
            return null;
        }
        return new ReconnectTarget(scenarioGroupId, operationId);
    }

    private record ReconnectTarget(Long scenarioGroupId, String operationId) {
    }

    private void validateEventIds(List<Long> miEventIds) {
        if (miEventIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "miEventIds에는 양의 정수만 허용됩니다.");
        }
        if (miEventIds.stream().distinct().count() != miEventIds.size()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "miEventIds에 중복된 이벤트가 있습니다.");
        }
    }

    private interface StreamOpener {
        void open(SseEmitter emitter, CompletableFuture<Void> connectGate);
    }

    /**
     * 주어진 emitter에 스트림 개설. 전용 스레드에서 Agent 스트림을 소비시키고,
     * 응답 status 확정(connectGate)까지만 컨트롤러 스레드를 붙잡는다.
     * 연결 오류는 emitter에 손대지 않고 예외로 던지므로, 호출측이 같은 emitter로 재시도할 수 있다.
     */
    private void openStreamOn(SseEmitter emitter, StreamOpener opener, String action) {
        CompletableFuture<Void> connectGate = new CompletableFuture<>();

        streamExecutor.execute(() -> {
            try {
                opener.open(emitter, connectGate);
            } catch (Exception e) {
                if (!connectGate.isDone()) {
                    connectGate.completeExceptionally(e);
                } else {
                    log.warn("[Design V4] {} 중계 스레드 오류: {}", action, e.getMessage(), e);
                    try {
                        emitter.completeWithError(e);
                    } catch (Exception ignored) {
                        // 이미 완료된 emitter
                    }
                }
            }
        });

        awaitConnected(connectGate, action);
    }

    private void awaitConnected(CompletableFuture<Void> connectGate, String action) {
        try {
            connectGate.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AgentDesignApiException agentDesignApiException) {
                throw agentDesignApiException;
            }
            if (cause instanceof AppErrorException appErrorException) {
                throw appErrorException;
            }
            log.error("[Design V4] {} agent 연결 실패: {}", action, cause == null ? e.getMessage() : cause.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent stream connect failed (%s)".formatted(action));
        } catch (TimeoutException e) {
            log.error("[Design V4] {} agent 연결 타임아웃", action);
            throw AppErrorException.of(ResultCode.Error.API_TIMEOUT, "Agent stream connect timeout (%s)".formatted(action));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent stream connect interrupted (%s)".formatted(action));
        }
    }

}
