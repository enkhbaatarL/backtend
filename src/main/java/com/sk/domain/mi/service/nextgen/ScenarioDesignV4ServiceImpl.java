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
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStatusRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioValidationCache;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignValidationStore;
import com.sk.domain.mi.service.nextgen.sse.ScenarioDesignStreamExecutor;
import com.sk.domain.mi.service.nextgen.support.FailedCreatingGroupResolver;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignPresentationMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignTableMapper;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.ScenarioDesignAgentSseClient;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupEvent;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationType;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupOperationRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
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
 * 재연결 replay 시작점은 FE 커서다 — 요청의 lastSequence를 Agent Last-Event-ID로 전달해
 * 그 다음 sequence부터 replay하고, 커서가 없으면(브라우저 재시작 등) 0부터 전체 replay한다.
 * FE는 연결 끊김/새로고침 후에도 같은 생성 API 재호출만으로 복구된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignV4ServiceImpl implements ScenarioDesignV4Service {

    /** 생성 평균 150초 + 검토/재시도 여유. FE 이탈 시 emitter 타임아웃으로 정리된다. */
    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60 * 1000;
    private static final long CONNECT_TIMEOUT_SECONDS = 10L;

    private final ScenarioDesignAgentSseClient sseClient;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupOperationRepository operationRepository;
    private final MiScenarioGroupEventRepository groupEventRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final FailedCreatingGroupResolver failedCreatingGroupResolver;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ScenarioDesignStreamExecutor streamExecutor;

    /**
     * [이벤트 조합 설계 상태 조회]
     * 선택한 이벤트 조합과 정확히 일치하는 그룹의 상태를 판정한다 (이벤트 목록 플로팅바 CTA 분기).
     */
    @Override
    public ScenarioDesignStatusRes getDesignStatus(List<Long> miEventIds) {

        validateEventIds(miEventIds);

        Set<Long> requested = Set.copyOf(miEventIds);

        // 요청 이벤트 ID 하나로 후보를 좁히고, 비교로 확정
        List<MiScenarioGroup> matched = groupEventRepository
            .findActiveWithGroupByMiEventId(miEventIds.get(0))
            .stream()
            .map(MiScenarioGroupEvent::getScenarioGroup)
            .filter(group -> requested.equals(Set.copyOf(groupEventRepository.findActiveEventIdsByScenarioGroupId(group.getScenarioGroupId()))))
            .toList();

        MiScenarioGroup creating = matched.stream()
            .filter(group -> group.getStatus() == ScenarioGroupStatus.CREATING)
            .filter(group -> !isFailedCreating(group.getScenarioGroupId()))
            .findFirst()
            .orElse(null);

        // [생성중] 응답 (진행자 미상 그룹은 mine=null — 타인 점유 잠금으로 오표기하지 않는다)
        if (creating != null) {
            Long userId = authorizationService.getUserNo().longValue();
            return ScenarioDesignStatusRes.creating(
                creating.getScenarioGroupId(),
                creating.getStage(),
                creating.getRequestUserId(),
                resolveUserName(creating.getRequestUserId()),
                creating.getRequestUserId() == null ? null : userId.equals(creating.getRequestUserId()));
        }

        // [생성완료 || 생성가능] 응답
        return matched.stream()
            .filter(group -> group.getStatus() == ScenarioGroupStatus.CREATED || group.getStatus() == ScenarioGroupStatus.REALIZED)
            .findFirst()
            .map(group -> ScenarioDesignStatusRes.created(group.getScenarioGroupId()))
            .orElseGet(ScenarioDesignStatusRes::designable);
    }

    /**
     * 실패 방치(좀비) 그룹 판정 (FailedCreatingGroupResolver 단일 지점 위임).
     */
    private boolean isFailedCreating(Long scenarioGroupId) {
        return failedCreatingGroupResolver.isFailedCreating(scenarioGroupId);
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return null;
        return userRepository.findByUserNoInAndIsDeletedFalse(List.of(userId.intValue())).stream()
            .findFirst()
            .map(User::getUserNm)
            .orElse(null);
    }

    /**
     * [설계 시작 SSE 연결]
     * 이벤트 1~5개 검증 후 Agent 생성 스트림을 개설해 FE로 직결 중계
     * 개수/중복/양수는 B.E 선검증으로 Agent의 FastAPI 422 응답이 FE로 새는 것을 막는다.
     */
    @Override
    public SseEmitter startDesignStream(ScenarioDesignStartV4Req request) {

        validateEventIds(request.miEventIds());
        Long userId = authorizationService.getUserNo().longValue();

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);

        ReconnectTarget active = findMyActiveOperation(userId, request.miEventIds());

        if (active != null) {
            String lastEventId = resolveLastEventId(request, active.operationId());
            log.info("[Design v4] - 설계 재연결 시도: scenarioGroupId={}, operationId={}, lastSequence={}", active.scenarioGroupId(), active.operationId(), lastEventId);

            openStreamOn(emitter,
                (target, connectGate) -> sseClient.pipeOperationStream(active.scenarioGroupId(), active.operationId(), lastEventId, target, connectGate),
                "resumeOperationStream");
            return emitter;
        }

        ScenarioGroupCreateV4AgentReq agentRequest = ScenarioGroupCreateV4AgentReq.of(request.miEventIds(), userId);

        try {
            openStreamOn(emitter, (target, connectGate) -> sseClient.pipeCreateStream(agentRequest, target, connectGate), "startDesignStream");
        } catch (AgentDesignApiException e) {
            ReconnectTarget reconnect = resolveReconnectTarget(e);

            if (reconnect == null) throw e;

            String lastEventId = resolveLastEventId(request, reconnect.operationId());
            log.info("[Design V4] 동일 조합 작업 진행 중 - 자동 재연결: scenarioGroupId={}, operationId={}, lastSequence={}", reconnect.scenarioGroupId(), reconnect.operationId(), lastEventId);
            openStreamOn(emitter, (target, connectGate) -> sseClient.pipeOperationStream(reconnect.scenarioGroupId(), reconnect.operationId(), lastEventId, target, connectGate), "reconnectOperationStream");
        }
        return emitter;
    }

    /**
     * FE 커서 적용 판정
     * 재연결 대상 operation과 쌍의 operationId가 일치할 때만 Last-Event-ID로 적용하고,
     * 불일치·미제공이면 0부터 전체 replay한다 (다른 operation에 커서를 적용하면 초반 이벤트가 유실된다).
     */
    private String resolveLastEventId(ScenarioDesignStartV4Req request, String targetOperationId) {
        if (request.lastSequence() == null || request.operationId() == null || !request.operationId().equals(targetOperationId)) {
            return null;
        }
        return String.valueOf(request.lastSequence());
    }

    /**
     * [설계 화면 데이터 조회] Agent presentation을 B.E 계약으로 가공해 내린다 — 설계 화면의 단일 조회 API.
     * Agent 응답에 없는 화면 재료를 B.E에서 합성한다: 그룹 구성 이벤트 목록(매핑 테이블),
     * operation 실패 사유(lastError, 종료 상태일 때만), 설계 세션 상태(designSession — 그룹 엔티티 + Redis 검토 캐시).
     * 검토를 통과한 편집본(DB 미반영)이 캐시에 있으면 table을 편집본으로 오버레이하므로,
     * 검토 후 재조회도 이 API 하나로 충분하다 (세션 상태 조회 API의 역할 흡수 — 2026-08-14 단일화 결정).
     * 오버레이는 step1 편집 단계에서만 적용한다 — Signpost 산출 이후는 Agent 영속 상태가 정본이다.
     */
    @Override
    public ScenarioDesignPresentationRes getPresentation(Long scenarioGroupId, String operationId) {
        JsonNode agentBody;

        try {
            agentBody = scenarioDesignAgentApi.getPresentation(scenarioGroupId, operationId);
        } catch (RestClientResponseException e) {
            // 선언형 클라이언트 오류를 기존 계약(AgentDesignApiException -> 전용 핸들러)으로 번역해 FE 오류 응답 모양을 유지한다
            throw ScenarioDesignAgentSseClient.translateError(e.getStatusCode().value(), e.getResponseBodyAsString());
        }

        List<ScenarioGroupBaseEventRow> baseEvents = scenarioGroupRepository.findBaseEventsByScenarioGroupId(scenarioGroupId);

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId).orElse(null);
        // 검토 캐시는 step1 편집 단계에서만 유효하다 — Signpost 산출부터는 편집본이 이미 영속화됐고
        // Agent가 narrative/확률을 산출하므로, 남은 캐시로 table을 덮거나 낡은 검토 결과를 내리면 안 된다 (v1 getDesign의 stage 게이트와 동일)
        ScenarioValidationCache cache = isStep1EditStage(group)
            ? validationStore.find(scenarioGroupId).orElse(null)
            : null;
        DesignTable overlayTable = cache == null ? null : ScenarioDesignTableMapper.toDesignTable(cache.table());

        return ScenarioDesignPresentationMapper.toRes(
            agentBody,
            baseEvents,
            resolveLastError(agentBody),
            toRefinementState(group, cache),
            overlayTable,
            loadSignpostsByScenario(group));
    }

    /** step1 편집 단계(검토 캐시 오버레이 대상) 여부 — 생성 완료 후 Signpost 산출 전까지. */
    private boolean isStep1EditStage(MiScenarioGroup group) {
        return group != null
            && (group.getStage() == ScenarioGroupStage.SCENARIO_CREATED || group.getStage() == ScenarioGroupStage.SCENARIO_VALIDATED);
    }

    /**
     * 정련 상태 합성
     * 단계/점유/그룹 메타는 그룹 엔티티, 검토 결과는 Redis 캐시(step1 게이트 통과분)가 원천이다.
     */
    private ScenarioDesignPresentationRes.ScenarioGroupRefinementState toRefinementState(MiScenarioGroup group, ScenarioValidationCache cache) {

        if (group == null) return null;

        ScenarioDesignPresentationRes.DesignSessionValidation validation = cache == null
            ? null
            : new ScenarioDesignPresentationRes.DesignSessionValidation(cache.isValid(), cache.reason());

        Long userId = authorizationService.getUserNo().longValue();

        return ScenarioDesignPresentationRes.ScenarioGroupRefinementState.of(
            group.getStage(),
            group.getStatus() == ScenarioGroupStatus.CREATING,
            userId.equals(group.getRequestUserId()),
            group.getRequestUserId(),
            validation,
            new ScenarioDesignPresentationRes.ScenarioGroupMeta(group.getTitle(), group.getSummary(), group.getDescription()));
    }

    /**
     * 행별 Signpost 목록 합성
     */
    private Map<Long, List<ScenarioSignpostDefinitionViewRes>> loadSignpostsByScenario(MiScenarioGroup group) {
        if (group == null || (group.getStage() != ScenarioGroupStage.SIGNPOST_CREATED && group.getStage() != ScenarioGroupStage.COMPLETED)) {
            return Map.of();
        }
        return scenarioGroupRepository.findSignpostEntitiesByGroupId(group.getScenarioGroupId()).stream()
            .filter(sp -> Boolean.TRUE.equals(sp.getIsUsed()))
            .sorted(Comparator.comparing(sp -> sp.getDisplayOrder() == null ? Integer.MAX_VALUE : sp.getDisplayOrder()))
            .collect(Collectors.groupingBy(
                sp -> sp.getScenario().getScenarioId(),
                Collectors.mapping(sp -> ScenarioSignpostDefinitionViewRes.of(
                    sp.getSignpostId(), sp.getLabel(), sp.getProbabilityDirection(),
                    sp.getProbabilityImpactScore(),
                    sp.getStrength(), sp.getSignpostReason(), sp.getDisplayOrder()),
                    Collectors.toList())));
    }

    /**
     * Agent 응답의 operation이 실패 종료 상태면 operation 테이블에서 실패 사유 원문을 읽어온다.
     * */
    private JsonNode resolveLastError(JsonNode agentBody) {
        JsonNode operation = agentBody.path("operation");
        String operationId = operation.path("operationId").asText(null);
        String status = operation.path("status").asText(null);
        if (operationId == null || !DesignOperationStatus.isFailedTerminal(status)) {
            return null;
        }
        String lastError = operationRepository.findById(operationId)
            .map(MiScenarioGroupOperation::getLastError)
            .orElse(null);

        if (lastError == null || lastError.isBlank()) return null;

        try {
            return JsonUtil.fromJson(lastError, JsonNode.class);
        } catch (Exception e) {
            log.warn("[Design V4] lastError 파싱 실패: operationId={}, {}", operationId, e.getMessage());
            return null;
        }
    }

    /**
     * 요청 사용자 진행중 작업 중 요청 이벤트 조합과 정확히 일치하는 그룹의 작업을 찾는다 (선조회 정상 경로).
     */
    private ReconnectTarget findMyActiveOperation(Long userId, List<Long> miEventIds) {
        Set<Long> requested = Set.copyOf(miEventIds);
        return operationRepository
            .findActiveWithGroupByRequestUserId(userId, DesignOperationStatus.activeCodes())
            .stream()
            .filter(operation -> DesignOperationType.CREATE == DesignOperationType.fromNullable(operation.getOperationType()))
            .filter(operation -> requested.equals(Set.copyOf(groupEventRepository.findActiveEventIdsByScenarioGroupId(operation.getScenarioGroup().getScenarioGroupId()))))
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
        if (miEventIds == null || miEventIds.isEmpty() || miEventIds.size() > 5) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "`miEventIds`는 1~5개여야 합니다.");
        }
        if (miEventIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "`miEventIds`에는 양의 정수만 허용됩니다.");
        }
        if (miEventIds.stream().distinct().count() != miEventIds.size()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "`miEventIds`에 중복된 이벤트가 있습니다.");
        }
    }

    private interface StreamOpener {
        void open(SseEmitter emitter, CompletableFuture<Void> connectGate);
    }

    /**
     * 주어진 emitter에 스트림 개설.
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

        awaitConnected(emitter, connectGate, action);
    }

    /**
     * 연결 확정 대기.
     * Agent JSON 오류(ExecutionException)는 중계 스레드가 이미 종료된 상태라 emitter를 재시도용으로 보존하고,
     * 타임아웃/인터럽트는 중계 스레드가 아직 살아 있어 뒤늦게 열릴 수 있으므로 emitter를 완료시켜
     * 고아 emitter에 쓰는 것을 차단한다.
     */
    private void awaitConnected(SseEmitter emitter, CompletableFuture<Void> connectGate, String action) {
        try {
            connectGate.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AgentDesignApiException agentDesignApiException) throw agentDesignApiException;
            if (cause instanceof AppErrorException appErrorException) throw appErrorException;
            log.error("[Design V4] {} agent 연결 실패: {}", action, cause == null ? e.getMessage() : cause.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent stream connect failed (%s)".formatted(action));
        } catch (TimeoutException e) {
            safeComplete(emitter);
            log.error("[Design V4] {} agent 연결 타임아웃", action);
            throw AppErrorException.of(ResultCode.Error.API_TIMEOUT, "Agent stream connect timeout (%s)".formatted(action));
        } catch (InterruptedException e) {
            safeComplete(emitter);
            Thread.currentThread().interrupt();
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent stream connect interrupted (%s)".formatted(action));
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 이미 완료된 emitter
        }
    }

}
