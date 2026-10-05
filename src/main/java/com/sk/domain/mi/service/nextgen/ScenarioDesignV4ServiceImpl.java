package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AgentDesignApiException;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartV4Req;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.AdditionalDimensionsRes;
import com.sk.domain.mi.dto.response.nextgen.design.DesignTable;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignStatusRes;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioValidationCache;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignValidationStore;
import com.sk.domain.mi.service.nextgen.sse.ScenarioDesignStreamExecutor;
import com.sk.domain.mi.service.nextgen.support.FailedCreatingGroupResolver;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignAgentMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignTableMapper;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.ScenarioDesignAgentSseClient;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.client.dto.response.AdditionalDimensionsAgentRes;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationType;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupOperationRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseEventRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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
 * 동일 이벤트 조합의 그룹 중복 생성을 허용한다 — 생성 요청은 항상 새 그룹을 만들고,
 * 조합으로는 재연결 대상을 특정할 수 없으므로 재연결은 그룹 ID 기준의 별도 API로 받는다.
 * 재연결 replay 시작점은 FE 커서다 — lastSequence를 Agent Last-Event-ID로 전달해
 * 그 다음 sequence부터 replay하고, 커서가 없으면(브라우저 재시작 등) 0부터 전체 replay한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioDesignV4ServiceImpl implements ScenarioDesignV4Service {

    /** 생성 평균 150초 + 검토/재시도 여유. FE 이탈 시 emitter 타임아웃으로 정리된다. */
    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60 * 1000;
    private static final long CONNECT_TIMEOUT_SECONDS = 10L;

    /** 진행중 플래그 합성 대상 타입  */
    private static final List<String> REFINEMENT_OPERATION_TYPES =
        List.of(DesignOperationType.VALIDATION.getCode(), DesignOperationType.SIGNPOST.getCode());

    /** 정련 작업 진행중 판정 상한 */
    private static final Duration REFINEMENT_TASK_TIMEOUT = Duration.ofMinutes(15);

    private final ScenarioDesignAgentSseClient sseClient;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupOperationRepository operationRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final FailedCreatingGroupResolver failedCreatingGroupResolver;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final ScenarioDesignStreamExecutor streamExecutor;

    /**
     * [이벤트 조합 설계 상태 조회]
     * 선택한 이벤트 조합과 구성이 같은 그룹 목록을 조회한다 (이벤트 목록 플로팅바의 동일 조합 안내).
     * 단건 선택은 동일 조합 검사 대상이 아니므로 빈 목록을 반환한다.
     */
    @Override
    public ScenarioDesignStatusRes getDesignStatus(List<Long> miEventIds) {

        validateEventIds(miEventIds);

        if (miEventIds.size() == 1) return ScenarioDesignStatusRes.of(List.of());

        List<MiScenarioGroup> matched = scenarioGroupRepository.findActiveByExactEventSet(miEventIds, miEventIds.size());

        // 실패 방치(좀비) 그룹 제외 (FailedCreatingGroupResolver 단일 지점 위임)
        Set<Long> zombieGroupIds = failedCreatingGroupResolver.resolve(matched.stream()
            .filter(group -> group.getStatus() == ScenarioGroupStatus.CREATING)
            .map(MiScenarioGroup::getScenarioGroupId)
            .toList());

        List<MiScenarioGroup> groups = matched.stream()
            .filter(group -> !zombieGroupIds.contains(group.getScenarioGroupId()))
            .toList();

        Map<Long, String> userNames = resolveUserNames(groups.stream().map(MiScenarioGroup::getRequestUserId).toList());
        Long userId = authorizationService.getUserNo().longValue();

        // mine: 진행자 미상(request_usr_id null)은 Agent 적재 결함이므로 fail-closed로 타인 진행 취급한다.
        return ScenarioDesignStatusRes.of(groups.stream()
            .map(group -> ScenarioDesignStatusRes.SameCombinationGroup.of(
                group.getScenarioGroupId(),
                group.getTitle(),
                userNames.get(group.getRequestUserId()),
                group.getCreateDt(),
                group.getStatus(),
                group.getStage(),
                userId.equals(group.getRequestUserId())))
            .toList());
    }

    private Map<Long, String> resolveUserNames(List<Long> userIds) {
        List<Integer> userNos = userIds.stream().filter(Objects::nonNull).distinct().map(Long::intValue).toList();
        if (userNos.isEmpty()) return Map.of();
        return userRepository.findByUserNoInAndIsDeletedFalse(userNos).stream()
            .collect(Collectors.toMap(user -> user.getUserNo().longValue(), User::getUserNm, (first, second) -> first));
    }

    /**
     * [설계 시작 SSE 연결]
     * 이벤트 1~5개 검증 후 Agent 생성 스트림을 개설해 FE로 직결 중계 (동일 조합 그룹이 있어도 항상 새로 생성)
     * 개수/중복/양수는 B.E 선검증으로 Agent의 FastAPI 422 응답이 FE로 새는 것을 막는다.
     */
    @Override
    public SseEmitter startDesignStream(ScenarioDesignStartV4Req request) {

        validateEventIds(request.miEventIds());
        Long userId = authorizationService.getUserNo().longValue();

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        ScenarioGroupCreateV4AgentReq agentRequest = ScenarioGroupCreateV4AgentReq.of(request.miEventIds(), userId);

        try {
            openStreamOn(emitter, (target, connectGate) -> sseClient.pipeCreateStream(agentRequest, target, connectGate), "startDesignStream");
        } catch (AgentDesignApiException e) {
            log.warn("[Design V4] 시나리오 그룹 생성 거절: miEventIds={}, userId={}, status={}, agentMessage={}, agentData={}",
                request.miEventIds(), userId, e.getHttpStatus().value(), e.getAgentMessage(), e.getAgentData());
            throw e;
        }
        return emitter;
    }

    /**
     * [설계 생성 스트림 재연결]
     * 그룹의 진행중 create operation 스트림에 재연결한다. 진행자 본인만 허용한다.
     */
    @Override
    public SseEmitter resumeDesignStream(Long scenarioGroupId, Integer lastSequence) {

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        Long userId = authorizationService.getUserNo().longValue();
        if (!userId.equals(group.getRequestUserId())) {
            throw AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.BIZ_RULE_VIOLATION, "다른 사용자가 진행 중인 설계 세션입니다: %d".formatted(scenarioGroupId));
        }

        MiScenarioGroupOperation operation = operationRepository
            .findFirstByScenarioGroup_ScenarioGroupIdAndOperationTypeInAndStatusInAndIsDeletedFalseOrderByOperationIdDesc(
                scenarioGroupId, List.of(DesignOperationType.CREATE.getCode()), DesignOperationStatus.activeCodes())
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "진행 중인 생성 작업이 없습니다: %d".formatted(scenarioGroupId)));

        String operationId = operation.getOperationId();
        String lastEventId = lastSequence == null ? null : String.valueOf(lastSequence);
        log.info("[Design V4] 생성 스트림 재연결: scenarioGroupId={}, operationId={}, lastSequence={}", scenarioGroupId, operationId, lastEventId);

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        openStreamOn(emitter,
            (target, connectGate) -> sseClient.pipeOperationStream(scenarioGroupId, operationId, lastEventId, target, connectGate),
            "resumeDesignStream");
        return emitter;
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
        ScenarioPresentationAgentRes source =
            callAgent(() -> scenarioDesignAgentApi.getPresentation(scenarioGroupId, operationId), "getPresentation");

        List<ScenarioGroupBaseEventRow> baseEvents = scenarioGroupRepository.findBaseEventsByScenarioGroupId(scenarioGroupId);

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId).orElse(null);
        // 검토 캐시는 step1 편집 단계에서만 유효하다 — Signpost 산출부터는 편집본이 이미 영속화됐고
        // Agent가 narrative/확률을 산출하므로, 남은 캐시로 table을 덮거나 낡은 검토 결과를 내리면 안 된다 (v1 getDesign의 stage 게이트와 동일)
        ScenarioValidationCache cache = isStep1EditStage(group)
            ? validationStore.find(scenarioGroupId).orElse(null)
            : null;
        DesignTable overlayTable = cache == null ? null : ScenarioDesignTableMapper.toDesignTable(cache.table());

        return ScenarioDesignAgentMapper.toRes(
            source,
            baseEvents,
            resolveLastError(source),
            toRefinementState(group, cache, resolveRunningRefinementTask(scenarioGroupId)),
            overlayTable,
            loadSignpostsByScenario(group));
    }

    /**
     * 진행 중인 정련 작업(검토/Signpost) 판정 — Agent가 접수 시 적재하는 operation 테이블을 읽어 합성한다.
     * 재진입 시 stage만으로는 "요청 전"과 "실행 중"을 구분할 수 없어(전이는 콜백 시점), FE 스피너 표시의 원천으로 쓴다.
     * 상한(REFINEMENT_TASK_TIMEOUT)을 넘긴 running은 콜백 유실 방치로 보고 진행중으로 치지 않는다.
     * operation 테이블 시각은 Agent가 UTC로 적재하므로 KST 변환 후 현재 시각과 비교한다.
     */
    private DesignOperationType resolveRunningRefinementTask(Long scenarioGroupId) {
        return operationRepository
            .findFirstByScenarioGroup_ScenarioGroupIdAndOperationTypeInAndStatusInAndIsDeletedFalseOrderByOperationIdDesc(
                scenarioGroupId, REFINEMENT_OPERATION_TYPES, DesignOperationStatus.activeCodes())
            .filter(operation -> operation.getCreateDt() != null
                && DateUtil.utcToKst(operation.getCreateDt()).isAfter(LocalDateTime.now().minus(REFINEMENT_TASK_TIMEOUT)))
            .map(operation -> DesignOperationType.fromNullable(operation.getOperationType()))
            .orElse(null);
    }

    /**
     * [추가 분기항목 후보 조회]
     * Agent 프록시 (step1 편집의 + 버튼).
     * 후보는 시나리오 그룹 생성 중 Agent가 background로 비동기 생성하므로 이 API는 조회 전용이며,
     * FE는 generationStatus가 queued/running인 동안 재조회(polling)하고 unavailable 계열이면 후보 영역을 비활성화한다..
     */
    @Override
    public AdditionalDimensionsRes getAdditionalDimensions(Long scenarioGroupId) {
        scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %d".formatted(scenarioGroupId)));

        AdditionalDimensionsAgentRes agentRes =
            callAgent(() -> scenarioDesignAgentApi.getAdditionalDimensions(scenarioGroupId), "getAdditionalDimensions");

        return ScenarioDesignAgentMapper.toAdditionalDimensions(agentRes);
    }

    /**
     * Agent 호출을 감싸 통신 예외를 통제된 외부 API 에러로 변환한다 (설계 Impl callAgent 패턴 복사).
     * HTTP 오류 응답은 500 + 표준 메시지(API-004)로 내리고 Agent 원문은 응답 data와 로그에 남기며,
     * 타임아웃은 API_TIMEOUT, 연결 실패는 API_SERVICE_UNAVAILABLE, 그 외 호출 실패는 API_CALL_FAILED로 구분한다.
     */
    private <T> T callAgent(Supplier<T> call, String action) {
        try {
            return call.get();
        } catch (ApiErrorException e) {
            // 공통 RestClient 빌더의 오류 상태 핸들러가 HTTP 오류를 ApiErrorException(status + body 원문)으로 던진다
            log.error("[Design Agent] {} 응답 오류: status={}, body={}", action, e.getStatusCode(), e.getMessage(), e);
            throw AgentDesignApiException.internalFromAgentBody(e.getMessage());
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[Design Agent] {} 통신 오류: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(code, e);
        } catch (RestClientException e) {
            log.error("[Design Agent] {} 호출 실패: {}", action, e.getMessage(), e);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        }
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
    private ScenarioDesignPresentationRes.ScenarioGroupRefinementState toRefinementState(MiScenarioGroup group, ScenarioValidationCache cache, DesignOperationType runningTask) {

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
            runningTask,
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
    private JsonNode resolveLastError(ScenarioPresentationAgentRes source) {
        ScenarioPresentationAgentRes.Operation operation = source.operation();
        String operationId = operation == null ? null : operation.operationId();
        if (operationId == null || !DesignOperationStatus.isFailedTerminal(operation.status())) {
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
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
        } catch (TimeoutException e) {
            safeComplete(emitter);
            log.error("[Design V4] {} agent 연결 타임아웃", action);
            throw ApiErrorException.of(ResultCode.Error.API_TIMEOUT, e);
        } catch (InterruptedException e) {
            safeComplete(emitter);
            Thread.currentThread().interrupt();
            log.error("[Design V4] {} agent 연결 대기 인터럽트", action);
            throw ApiErrorException.of(ResultCode.Error.API_CALL_FAILED, e);
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
