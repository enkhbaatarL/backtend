package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
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
import com.sk.domain.shared.client.ScenarioDesignAgentApi;
import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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

            if (scenarioGroupRepository.existsByMiEvent_MiEventIdAndIsDeletedFalse(miEventId)) {
                throw AppErrorException.of(ResultCode.Error.BIZ_DESIGN_OCCUPIED, "이미 설계 중이거나 완료된 이벤트입니다. (eventId=%d)".formatted(miEventId));
            }

            ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.createScenarioGroup(ScenarioGroupCreateAgentReq.of(miEventId, userId)), "startDesign");
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

        // 분기항목 헤더: event_type_l2 template + dimension_selected (그룹 상세/이벤트 연관시나리오와 동일)
        Long eventTypeL2Id = group.getEventTypeL2().getEventTypeL2Id();
        GroupDimensionTemplate template = ScenarioDimensionMapper
            .parseTemplates(scenarioGroupRepository.findTemplatesByEventTypeL2Ids(List.of(eventTypeL2Id)))
            .getOrDefault(eventTypeL2Id, GroupDimensionTemplate.EMPTY);

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
                    template.dimensions(selectedKeys, s.getDimensionValues()),
                    s.getProbabilityScore() == null ? null : s.getProbabilityScore().intValue(),
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
                    sp.getProbabilityImpactScore() == null ? null : sp.getProbabilityImpactScore().intValue(),
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

        ScenarioConfirmationReq.GroupEdit edit = request.scenarioGroup();
        ScenarioConfirmationAgentReq.GroupEdit groupEdit = edit == null ? null : new ScenarioConfirmationAgentReq.GroupEdit(edit.title(), edit.summary(), edit.description());

        ScenarioDesignTable table = ScenarioDesignTableMapper.toAgentConfirmTable(request.table());

        ScenarioDesignAgentRes res = callAgent(() -> scenarioDesignAgentApi.confirm(scenarioGroupId, ScenarioConfirmationAgentReq.of(groupEdit, table)), "confirmDesign");

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
        } catch (ResourceAccessException e) {
            ResultCode.Error code = e.getCause() instanceof SocketTimeoutException ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[Design Agent] {} 통신 오류: {}", action, e.getMessage(), e);
            throw AppErrorException.of(code, "Agent communication error (%s)".formatted(action));
        } catch (RestClientResponseException e) {
            log.error("[Design Agent] {} 응답 오류: status={}, body={}", action, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "Agent response error (%s)".formatted(action));
        } catch (RestClientException e) {
            log.error("[Design Agent] {} 호출 실패: {}", action, e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent call failed (%s)".formatted(action));
        }
    }

    /**
     * [전체 초기화] 사용자 편집/검토 결과(Redis 검토 캐시)를 버리고 DB 생성 초기값으로 되돌린다.
     * <p>Agent 호출 없이 캐시만 삭제하고 세션 상태를 재조회한다 — 캐시가 없어지므로 {@link #getDesign}는
     * step1을 DB 시나리오(마지막 적용 상태)로 구성하고 validation은 null이 된다.
     */
    @Override
    public ScenarioDesignStateRes reset(Long scenarioGroupId) {
        validationStore.delete(scenarioGroupId);
        failureStore.delete(scenarioGroupId);
        return getDesign(scenarioGroupId);
    }

}
