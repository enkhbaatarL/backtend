package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AgentDesignApiException;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioConfirmationReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationReq;
import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import com.sk.domain.mi.dto.response.nextgen.design.*;
import com.sk.domain.mi.service.nextgen.design.EventDesignLockStore;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignFailureStore;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignValidationStore;
import com.sk.domain.mi.service.nextgen.support.ScenarioDesignTableMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper;
import com.sk.domain.mi.service.nextgen.support.ScenarioDimensionMapper.GroupDimensionTemplate;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupEventRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioDesignServiceImpl implements ScenarioDesignService {

    private final MiScenarioGroupRepository scenarioGroupRepository;
    private final MiScenarioGroupEventRepository scenarioGroupEventRepository;
    private final ScenarioDesignValidationStore validationStore;
    private final ScenarioDesignFailureStore failureStore;
    private final EventDesignLockStore eventDesignLockStore;
    private final ScenarioDesignAgentApi scenarioDesignAgentApi;
    private final AuthorizationService authorizationService;

    /**
     * [설계 시작] Agent 바이패스
     *  - 그룹/시나리오 생성 및 DB 적재는 Agent가 수행
     *  - 점유 가드 2단:
     *    1. Redis NX 락 — 같은 이벤트 동시 설계 시작의 경쟁 창(DB row 생기기 전)을 직렬화
     *    2. DB 활성그룹 선검사 — 이미 점유된 이벤트의 재시작을 BE에서 차단(순차 호출 대비)
     *  - 락은 이 호출 동안만 보유하고 finally에서 즉시 해제 -> 그룹 생성 이후의 점유 지속/해제는 DB(scenarioDesignStatus)가 인계
     */
    @Override
    public ScenarioDesignStageRes startDesign(ScenarioDesignStartReq request) {
        Long userId = authorizationService.getUserNo().longValue();
        Long miEventId = request.miEventId();

        String lockToken = eventDesignLockStore.tryAcquire(miEventId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.BIZ_DESIGN_OCCUPIED, "다른 사용자가 설계를 시작하고 있습니다. (eventId=%d)".formatted(miEventId)));

        try {

            if (scenarioGroupEventRepository.existsByMiEvent_MiEventIdAndIsDeletedFalseAndScenarioGroup_IsDeletedFalse(miEventId)) {
                throw AppErrorException.of(ResultCode.Error.BIZ_DESIGN_OCCUPIED, "이미 설계 중이거나 완료된 이벤트입니다. (eventId=%d)".formatted(miEventId));
            }

            // 구 v1 create 경로는 Agent에서 제거됨 — 콜백 호환 v4 API에 단일 이벤트를 배열로 감싸 전달 (실측 OpenAPI 기준)
            ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.createScenarioGroup(ScenarioGroupCreateV4AgentReq.of(List.of(miEventId), userId)), "startDesign");
            return toStageRes(res);

        } finally {
            // lock 해제
            eventDesignLockStore.release(miEventId, lockToken);
        }

    }

    /**
     * [설계 세션 상태 조회]
     * DB 또는 Redis 기준으로 그룹/시나리오/Signpost를 조회
     */
    @Override
    public ScenarioDesignStateRes getDesign(Long scenarioGroupId) {

        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        ScenarioGroupStatus status = group.getStatus();
        ScenarioGroupStage stage = group.getStage();
        boolean occupied = status == ScenarioGroupStatus.CREATING;

        // 분기항목 헤더: v4는 그룹의 dimension_definitions, v1은 기준 이벤트 L2 template + dimension_selected
        Long eventTypeL2Id = scenarioGroupRepository.findEventTypeL2IdByScenarioGroupId(scenarioGroupId).orElse(null);
        GroupDimensionTemplate template = ScenarioDimensionMapper.resolve(
            group.getApiVersion(),
            group.getDimensionDefinitions(),
            eventTypeL2Id == null
                ? Map.of()
                : ScenarioDimensionMapper.parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(eventTypeL2Id))),
            eventTypeL2Id);

        List<String> selectedKeys = ScenarioDimensionMapper.parseSelectedKeys(group.getDimensionSelected());
        List<ScenarioDimensionColumnRes> columns = template.columns(selectedKeys);

        List<MiScenario> scenarios = scenarioGroupRepository.findScenarioEntitiesByGroupId(scenarioGroupId);
        Optional<ScenarioValidationCache> cache = validationStore.find(scenarioGroupId);

        boolean step1Stage = stage == ScenarioGroupStage.SCENARIO_CREATED || stage == ScenarioGroupStage.SCENARIO_VALIDATED;
        boolean step2Stage = stage == ScenarioGroupStage.SIGNPOST_CREATED || stage == ScenarioGroupStage.COMPLETED;

        // step1: 검토 캐시가 있으면 Agent reconcile 표로 오버레이, 없으면 DB 시나리오로 구성
        DesignTable step1 = null;
        if (step1Stage) {
            step1 = cache.map(ScenarioValidationCache::table)
                .map(ScenarioDesignTableMapper::toDesignTable)
                .orElseGet(() -> DesignTable.of(columns, toStep1Rows(scenarios, template, selectedKeys)));
        }

        // step2: One-view (그룹 메타 + narrative/probability/Signpost 포함 표)
        ScenarioDesignStateRes.OneView step2 = null;
        if (step2Stage) {
            Map<Long, List<ScenarioSignpostDefinitionViewRes>> signpostsByScenario = loadSignpostsByScenario(scenarioGroupId);
            List<DesignScenarioRow> rows = scenarios.stream()
                .map(s -> DesignScenarioRow.of(
                    s.getScenarioId(), s.getDisplayOrder(), s.getTitle(),
                    Boolean.TRUE.equals(s.getIsBaseline()),
                    null, null, null,
                    template.dimensions(selectedKeys, s.getDimensionValues()),
                    ScenarioProbabilityAllocator.toDisplayPercent(s.getProbabilityScore()),
                    s.getNarrative(),
                    signpostsByScenario.getOrDefault(s.getScenarioId(), List.of())))
                .toList();
            step2 = ScenarioDesignStateRes.OneView.of(
                group.getTitle(), group.getSummary(), group.getDescription(), DesignTable.of(columns, rows));
        }

        ScenarioDesignStateRes.Validation validation = cache
            .map(c -> ScenarioDesignStateRes.Validation.of(c.isValid(), c.reason()))
            .orElse(null);

        return ScenarioDesignStateRes.of(scenarioGroupId, status, stage, occupied, group.getRequestUserId(), step1, step2, validation);
    }

    /** DB 시나리오 → step1 행(분기항목/시나리오만, narrative/probability/Signpost는 null). */
    private List<DesignScenarioRow> toStep1Rows(List<MiScenario> scenarios, GroupDimensionTemplate template, List<String> selectedKeys) {
        return scenarios.stream()
            .map(s -> DesignScenarioRow.of(
                s.getScenarioId(), s.getDisplayOrder(), s.getTitle(),
                Boolean.TRUE.equals(s.getIsBaseline()),
                null, null, null,
                template.dimensions(selectedKeys, s.getDimensionValues()),
                null, null, null))
            .toList();
    }

    /** 그룹의 Signpost를 한 번에 조회해 시나리오ID별로 묶는다 (사용중인 것만, displayOrder 순). */
    private Map<Long, List<ScenarioSignpostDefinitionViewRes>> loadSignpostsByScenario(Long scenarioGroupId) {
        return scenarioGroupRepository.findSignpostEntitiesByGroupId(scenarioGroupId).stream()
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
     * 분기항목/분기기준 템플릿 조회 (항상 DB). 그룹의 event_type_l2 기준 template_json을 파싱해 내린다.
     */
    @Override
    public ScenarioDimensionTemplateRes getDimensionTemplates(Long scenarioGroupId) {
        ScenarioGroupDetailBaseRow base = scenarioGroupRepository.findScenarioGroupDetail(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        List<ScenarioDimensionTemplateRes.Item> items = scenarioGroupRepository
            .findTemplatesByEventTypeL2Ids(List.of(base.eventTypeL2Id())).stream()
            .findFirst()
            .map(row -> ScenarioDimensionMapper.parseTemplateItems(row.templateJson()))
            .orElseGet(List::of);

        return new ScenarioDimensionTemplateRes(items);
    }

    /**
     * [변경사항 검토] Agent 바이패스
     *  - 편집된 설계를 Agent 표로 변환해 검토를 요청
     */
    @Override
    public ScenarioDesignStageRes validateDesign(Long scenarioGroupId, ScenarioValidationReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.validate(scenarioGroupId, ScenarioValidationAgentReq.of(userId, table)), "validateDesign");

        return toStageRes(res);
    }

    /**
     * [Signpost 생성] Agent 바이패스
     *  - 검토가 완료된 설계를 Agent 표로 변환해 단계2 진입을 요청 (그룹/시나리오/Signpost 영속화)
     */
    @Override
    public ScenarioDesignStageRes createSignposts(Long scenarioGroupId, ScenarioSignpostsReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.createSignposts(scenarioGroupId, ScenarioSignpostsAgentReq.of(userId, table)), "createSignposts");

        return toStageRes(res);
    }

    /**
     * [최종 확정] Agent 바이패스 — 그룹 편집 + full 확정 표를 Agent에 전달해 확정/스냅샷 저장을 요청한다.
     * <p>확정은 콜백이 없어 이 응답이 최종(status=created/stage=completed)이며, 성공 시 검토 캐시(Redis)를 정리한다.
     * 확정/스냅샷 적재 주체는 Agent다(B.E는 DB에 쓰지 않음).
     */
    @Override
    public ScenarioDesignStageRes confirmDesign(Long scenarioGroupId, ScenarioConfirmationReq request) {

        requireEditableByCurrentUser(scenarioGroupId);

        Long userId = authorizationService.getUserNo().longValue();
        ScenarioConfirmationReq.GroupEdit edit = request.scenarioGroup();
        ScenarioConfirmationAgentReq.GroupEdit groupEdit = edit == null ? null : new ScenarioConfirmationAgentReq.GroupEdit(edit.title(), edit.summary(), edit.description());

        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentConfirmTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.confirm(scenarioGroupId, ScenarioConfirmationAgentReq.of(userId, groupEdit, table)), "confirmDesign");

        ScenarioDesignStageRes stageRes = toStageRes(res);

        // 확정 완료 → 검토/실패 캐시 정리 (콜백 없는 단계라 여기서 마무리)
        validationStore.delete(scenarioGroupId);
        failureStore.delete(scenarioGroupId);

        return stageRes;
    }

    private ScenarioDesignStageRes toStageRes(ScenarioDesignAgentRes res) {

        if (!res.isSuccess()) {
            throw AppErrorException.of(ResultCode.Error.BIZ_RULE_VIOLATION, "agent response: %s".formatted(JsonUtil.toJson(res.data())));
        }

        ScenarioDesignAgentRes.Data data = res.data();
        return ScenarioDesignStageRes.of(
            data.scenarioGroupId(),
            ScenarioGroupStatus.from(data.status()),
            ScenarioGroupStage.from(data.stage())
        );

    }

    /**
     * Agent 호출을 감싸 통신 예외(호스트 다운/타임아웃/HTTP 에러 status)를 통제된 외부 API 에러로 변환
     */
    private <T> T callAgent(Supplier<T> call, String action) {
        try {
            return call.get();
        } catch (ApiErrorException e) {
            // 공통 RestClient 빌더의 오류 상태 핸들러가 HTTP 오류를 ApiErrorException(status + body 원문)으로 던진다.
            // Agent 오류는 B.E-Agent 구간 문제라 FE에는 500 + 표준 메시지(API-004)로 내리고, 원문은 응답 data와 로그에 남긴다.
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

    /**
     * [전체 초기화] 사용자 편집/검토 결과(Redis 검토/실패 캐시)를 버린다.
     * Agent 호출 없이 캐시만 삭제한다. 이후 조회는 검토 결과가 없는 마지막 영속 상태로 돌아간다.
     * FE는 초기화 후 설계 화면 데이터 조회 API를 재조회한다 (행위 -> 재조회 패턴으로 통일).
     */
    @Override
    public void reset(Long scenarioGroupId) {
        requireEditableByCurrentUser(scenarioGroupId);
        validationStore.delete(scenarioGroupId);
        failureStore.delete(scenarioGroupId);
    }

    /**
     * 편집 소유권 검증 — 설계 세션의 진행자(그룹 request_usr_id)만 편집 API(검토/Signpost/확정/초기화)를 호출할 수 있다.
     * 잠금은 FE 표시만으로는 우회 가능하므로 서버에서 강제한다.
     * 진행자 미상(request_usr_id null)은 Agent 적재 결함이라 편집을 이어가면 오염이 커지므로 fail-closed로 전원 차단한다
     * (화면의 mine 판정과 동일 기준). 이 경우 그룹은 편집 불가가 되며 그룹 삭제 후 재설계로 회복한다.
     */
    private void requireEditableByCurrentUser(Long scenarioGroupId) {
        MiScenarioGroup group = scenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found: %s".formatted(scenarioGroupId)));

        Long userId = authorizationService.getUserNo().longValue();
        if (!userId.equals(group.getRequestUserId())) {
            throw AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.BIZ_RULE_VIOLATION, "다른 사용자가 진행 중인 설계 세션입니다: %d".formatted(scenarioGroupId));
        }
    }

}
