package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.ApiErrorException;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.exception.SystemErrorException;
import com.sk.domain.mi.dto.request.nextgen.GroupModelExecutionItemReq;
import com.sk.domain.mi.dto.request.nextgen.GroupModelExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelExecutionItemRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelExecutionRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamCompareRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalItemRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalResetRes;
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalsRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchive;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchiver;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchiver.ScenarioInputFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.GroupMiBaseResolver;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamCompareMerger;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamCompareMerger.CompareSource;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시나리오 파라미터 비교 팝업의 그룹 단위 제안 현황 조회.
 * 시나리오별 상태 사영은 단건 서비스의 getCurrentProposal을 그대로 재사용해
 * 단건 GET과 그룹 조회가 항상 같은 상태 규칙을 따르도록 한다.
 * 분기값과 차원 헤더는 그룹 상세 API가 제공하므로 이 응답에는 포함하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelParamProposalGroupServiceImpl implements ModelParamProposalGroupService {

    /**
     * 비교 그리드에 포함되는 "완료(제안 보유)" 상태 — 화면 상태 컬럼의 완료 그룹과 동일 집합.
     * 실행 실패도 확정 파라미터가 존재해 그리드 노출·재실행이 가능하므로 포함한다.
     */
    private static final Set<CurrentModelParamProposalState> PROPOSAL_HELD_STATES = Set.of(
        CurrentModelParamProposalState.PROPOSAL_READY,
        CurrentModelParamProposalState.CONFIRMED_PENDING_EXECUTION,
        CurrentModelParamProposalState.EXECUTING,
        CurrentModelParamProposalState.EXECUTED_CONFIRMED,
        CurrentModelParamProposalState.EXECUTION_FAILED
    );

    /** actv_scnr_id 생성식과 같은 진행 중 상태 */
    private static final List<ModelExecutionPreparationStatus> IN_FLIGHT_STATUSES = List.of(
        ModelExecutionPreparationStatus.REQUESTING,
        ModelExecutionPreparationStatus.PROPOSAL_PENDING,
        ModelExecutionPreparationStatus.CONFIRMING,
        ModelExecutionPreparationStatus.PREPARING_EXECUTION
    );
    private static final List<ModelExecutionPreparationStatus> RESET_CLOSE_STATUSES = List.of(
        ModelExecutionPreparationStatus.PROPOSAL_PENDING,
        ModelExecutionPreparationStatus.REQUEST_INTERRUPTED
    );
    private static final String RESET_CLOSE_REASON = "그룹 전체 초기화로 종료된 제안입니다.";

    private final MiScenarioGroupRepository miScenarioGroupRepository;
    private final MiScenarioRepository miScenarioRepository;
    private final ModelParamProposalService modelParamProposalService;
    private final ModelParamCompareMerger modelParamCompareMerger;
    private final ScenarioModelExecutionService scenarioModelExecutionService;
    private final GroupModelInputFilesArchiver groupModelInputFilesArchiver;
    private final GroupMiBaseResolver groupMiBaseResolver;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final AuthorizationService authorizationService;

    @Override
    public GroupModelParamProposalsRes getGroupProposals(Long scenarioGroupId) {
        MiScenarioGroup group = findGroup(scenarioGroupId);
        List<MiScenario> scenarios = findGroupScenarios(scenarioGroupId);

        // 재분배 확률은 그룹 도메인과 동일한 조회 시점 계산값 — 폐기 제외 비례 재분배(활성 합 100, 폐기=0)
        Map<Long, Integer> adjustedProbabilities = ScenarioProbabilityAllocator.redistribute(
            scenarios.stream()
                .map(scenario -> new ScenarioProbabilityAllocator.Item(
                    scenario.getScenarioId(),
                    scenario.getProbabilityScore(),
                    ScenarioDisplayStatus.from(scenario.getStatus()) == ScenarioDisplayStatus.DISCARDED
                ))
                .toList()
        );

        List<GroupModelParamProposalItemRes> items = scenarios.stream()
            .map(scenario -> GroupModelParamProposalItemRes.of(
                scenario,
                modelParamProposalService.getCurrentProposal(scenario.getScenarioId()),
                adjustedProbabilities.get(scenario.getScenarioId())
            ))
            .toList();

        return GroupModelParamProposalsRes.of(group, items, groupMiBaseResolver.status(scenarioGroupId));
    }

    /**
     * 그룹 전체 초기화 — 시나리오별로 RESET 행을 추가해 [생성] 전 상태로 되돌린다.
     * 이전 제안·실행 이력은 지우지 않는다. 그룹 MI Base가 비게 되어 다음 [생성]부터 current MI Base로 생성된다.
     * 생성 중·실행 중인 시나리오가 있으면 늦게 도착한 결과가 초기화를 무력화하므로 거절한다.
     */
    @Override
    @Transactional
    public GroupModelParamProposalResetRes resetGroupProposals(Long scenarioGroupId) {
        findGroup(scenarioGroupId);
        // 시나리오 상태 변경(실현/폐기)과 같은 잠금 — 생성·실행 접수와도 시나리오 행 잠금으로 직렬화된다
        List<MiScenario> scenarios = miScenarioGroupRepository.findScenarioEntitiesByGroupIdForUpdate(scenarioGroupId);

        List<Long> runningScenarioIds = scenarios.stream()
            .map(MiScenario::getScenarioId)
            .filter(this::isInProgress)
            .toList();

        if (!runningScenarioIds.isEmpty()) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "생성 중이거나 실행 중인 시나리오가 있어 초기화할 수 없습니다. scenarioIds: %s".formatted(runningScenarioIds)
            );
        }

        Integer actorUserNo = authorizationService.getUserNo();
        String actorLoginId = authorizationService.getSubject();
        List<Long> resetScenarioIds = new ArrayList<>();
        for (MiScenario scenario : scenarios) {
            Long scenarioId = scenario.getScenarioId();
            Optional<ModelExecutionPreparation> latest = modelExecutionPreparationRepository.findFirstByScenarioIdAndIsDeletedFalseOrderByExecPrepIdDesc(scenarioId);
            if (latest.isEmpty() || latest.get().getStatus() == ModelExecutionPreparationStatus.RESET) {
                continue;
            }
            closeOpenPreparations(scenarioId, actorLoginId);
            modelExecutionPreparationRepository.save(ModelExecutionPreparation.createReset(scenarioId, actorUserNo));
            resetScenarioIds.add(scenarioId);
        }

        log.info("Group model parameter proposals reset. scenarioGroupId={}, resetScenarioIds={}", scenarioGroupId, resetScenarioIds);
        return new GroupModelParamProposalResetRes(scenarioGroupId, resetScenarioIds);
    }

    /**
     * 생성 중·실행 접수 중·실행 중 판정. 현재 상태 조회(getCurrentProposal)의 GENERATING / CONFIRMED_PENDING_EXECUTION / EXECUTING과 같은 기준을
     * 원천 데이터로 판정한다 — 상태 조회는 케이스 정합성 예외를 던질 수 있어 그룹 전체 초기화를 막지 않도록 쓰지 않는다.
     */
    private boolean isInProgress(Long scenarioId) {
        // 실행 중: 제안 생성·실행 접수와 같은 RUNNING 케이스 기준 (multipart 실행 포함)
        if (scenarioCaseRepository.existsByScenarioIdAndExecTypeAndStatusAndIsDeletedFalse(
            scenarioId, FundamentalModelExecType.SCENARIO, FundamentalModelCaseStatus.RUNNING)) {
            return true;
        }
        return modelExecutionPreparationRepository.findLatestByScenarioIdAndStatuses(scenarioId, IN_FLIGHT_STATUSES)
            .map(this::isInProgress)
            .orElse(false);
    }

    /**
     * 진행 중 행(시나리오당 최대 1건, actv_scnr_id unique) 판정.
     * REQUESTING은 제안 ID가 붙지 않은 생성 중, CONFIRMING/PREPARING_EXECUTION은 실행 접수 중.
     * PROPOSAL_PENDING은 Agent 제안이 없거나 PROCESSING이면 생성 중, CONFIRMED면 실행 접수 중이고,
     * PROPOSED(완료)·FAILED/ABSTAINED(콜백 유실된 실패)는 초기화 대상이다.
     */
    private boolean isInProgress(ModelExecutionPreparation preparation) {
        if (preparation.getStatus() != ModelExecutionPreparationStatus.PROPOSAL_PENDING) {
            return true;
        }
        AgentModelParamProposalStatus agentStatus = preparation.getAgentProposalId() == null
            ? null
            : agentModelParamProposalRepository
                .findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(preparation.getAgentProposalId(), preparation.getScenarioId())
                .map(AgentModelParamProposal::getStatus)
                .orElse(null);
        return agentStatus == null
            || agentStatus == AgentModelParamProposalStatus.PROCESSING
            || agentStatus == AgentModelParamProposalStatus.CONFIRMED;
    }

    /**
     * 실행 전 제안(PROPOSAL_PENDING)과 통신 중단(REQUEST_INTERRUPTED) 행을 종료 상태로 닫는다.
     * PROPOSAL_PENDING은 진행 중 unique 제약(actv_scnr_id)을 잡고 있어 닫지 않으면 다음 [생성]이 막히고,
     * REQUEST_INTERRUPTED는 늦은 Agent 콜백으로 다시 진행 중이 될 수 있다. 제안 재생성 시 중단 행을 닫는 기존 처리와 같은 방식.
     */
    private void closeOpenPreparations(Long scenarioId, String actorLoginId) {
        modelExecutionPreparationRepository.findCallbackCandidates(scenarioId, RESET_CLOSE_STATUSES)
            .forEach(preparation -> modelExecutionPreparationRepository.transitionStatus(
                preparation.getExecPrepId(),
                RESET_CLOSE_STATUSES,
                ModelExecutionPreparationStatus.REQUEST_FAILED,
                RESET_CLOSE_REASON,
                actorLoginId,
                LocalDateTime.now()
            ));
    }

    @Override
    public GroupModelParamCompareRes getGroupCompare(Long scenarioGroupId) {
        findGroup(scenarioGroupId);

        List<CompareSource> sources = new ArrayList<>();
        for (MiScenario scenario : findGroupScenarios(scenarioGroupId)) {
            CurrentModelParamProposalRes current = modelParamProposalService.getCurrentProposal(scenario.getScenarioId());

            if (!PROPOSAL_HELD_STATES.contains(current.currentState()) || current.modelParamProposalId() == null) {
                continue;
            }

            sources.add(new CompareSource(
                scenario,
                modelParamProposalService.getProposal(scenario.getScenarioId(), current.modelParamProposalId())
            ));
        }
        return modelParamCompareMerger.merge(scenarioGroupId, sources);
    }

    /**
     * 선택 시나리오 N건을 순차로 실행 접수한다. 병합(POI) 메모리 부담 때문에 병렬 처리하지 않는다.
     * 부분 실패는 중단 없이 계속 진행하고 시나리오별 결과로 내린다 — 전체 응답은 HTTP 200.
     * 트랜잭션을 걸지 않는다. 각 단건 실행이 자체 트랜잭션 경계(confirm-병합-접수)를 관리한다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GroupModelExecutionRes executeGroup(Long scenarioGroupId, GroupModelExecutionReq request) {
        findGroup(scenarioGroupId);
        validateExecutionTargets(scenarioGroupId, request);

        List<GroupModelExecutionItemRes> results = new ArrayList<>();
        for (GroupModelExecutionItemReq item : request.executions()) {
            results.add(executeSingle(item));
        }
        return GroupModelExecutionRes.of(scenarioGroupId, results);
    }

    /**
     * 실행 완료 시나리오들의 실행 입력 파일(병합본)을 시나리오별 폴더 구조 zip으로 내린다.
     * 파일 메타가 불완전한 시나리오(role 누락·중복)는 경고 로그 후 제외한다.
     * 스토리지 IO가 길어질 수 있어 트랜잭션을 걸지 않는다.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GroupModelInputFilesArchive downloadInputFilesArchive(Long scenarioGroupId) {
        MiScenarioGroup group = findGroup(scenarioGroupId);

        List<ScenarioInputFiles> sources = new ArrayList<>();
        for (MiScenario scenario : findGroupScenarios(scenarioGroupId)) {
            CurrentModelParamProposalRes current = modelParamProposalService.getCurrentProposal(scenario.getScenarioId());

            if (current.executionInputFiles() == null) continue;

            if (!current.executionInputFiles().complete()) {
                log.warn("Skip scenario with incomplete execution input files in group archive. scenarioGroupId={}, scenarioId={}, missingRoles={}", scenarioGroupId, scenario.getScenarioId(), current.executionInputFiles().missingFileRoles());
                continue;
            }

            sources.add(new ScenarioInputFiles(
                scenario.getDisplayOrder(),
                scenario.getTitle(),
                current.executionInputFiles().files()
            ));

        }

        if (sources.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "실행 완료된 시나리오의 인풋 파일이 없습니다. scenarioGroupId: %d".formatted(scenarioGroupId));
        }

        String fileName = "인풋파일_%s_%s.zip".formatted(
            GroupModelInputFilesArchiver.sanitizeFileName(group.getTitle()),
            DateUtil.currentYYYYMMDDHHMMSS()
        );
        return new GroupModelInputFilesArchive(fileName, groupModelInputFilesArchiver.archive(sources));
    }

    private GroupModelExecutionItemRes executeSingle(GroupModelExecutionItemReq item) {

        try {

            return GroupModelExecutionItemRes.success(
                item.scenarioId(),
                scenarioModelExecutionService.submitModelExecution(item.scenarioId(), item.toSingleExecutionRequest())
            );

        } catch (RuntimeException e) {
            String errorMessage = errorMessageOf(e);
            log.warn("Group model execution item failed. scenarioId={}, modelParamProposalId={}, reason={}", item.scenarioId(), item.modelParamProposalId(), errorMessage, e);
            return GroupModelExecutionItemRes.failure(item.scenarioId(), errorCodeOf(e), errorMessage);
        }
    }

    /**
     * 시나리오별 실패 사유 메시지.
     * 상세 사유가 담기는 additionalMessage를 우선 사용하고, 없으면 getMessage()로 폴백한다.
     */
    private String errorMessageOf(RuntimeException e) {
        String additionalMessage = null;
        if (e instanceof AppErrorException appError) {
            additionalMessage = appError.getAdditionalMessage();
        } else if (e instanceof AppHttpStatusException httpStatusError) {
            additionalMessage = httpStatusError.getAdditionalMessage();
        } else if (e instanceof SystemErrorException systemError) {
            additionalMessage = systemError.getAdditionalMessage();
        }
        if (additionalMessage != null && !additionalMessage.isBlank()) {
            return additionalMessage;
        }
        return e.getMessage();
    }

    private String errorCodeOf(RuntimeException e) {
        if (e instanceof AppErrorException appError) {
            return appError.getErrorCode().name();
        }
        if (e instanceof AppHttpStatusException httpStatusError) {
            return httpStatusError.getErrorCode().name();
        }
        if (e instanceof SystemErrorException systemError) {
            return systemError.getErrorCode().name();
        }
        if (e instanceof ApiErrorException apiError) {
            return apiError.getErrorCode().name();
        }
        return ResultCode.Error.SYS_INTERNAL_ERROR.name();
    }

    private void validateExecutionTargets(Long scenarioGroupId, GroupModelExecutionReq request) {

        List<Long> requestedIds = request.executions().stream()
            .map(GroupModelExecutionItemReq::scenarioId)
            .toList();

        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);

        if (uniqueIds.size() != requestedIds.size()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "executions contain duplicated scenarioId.");
        }

        Set<Long> groupScenarioIds = findGroupScenarios(scenarioGroupId).stream()
            .map(MiScenario::getScenarioId)
            .collect(Collectors.toSet());

        Set<Long> outsiders = uniqueIds.stream()
            .filter(id -> !groupScenarioIds.contains(id))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!outsiders.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "executions contain scenarios outside the group. scenarioGroupId: %d, scenarioIds: %s".formatted(scenarioGroupId, outsiders));
        }
    }

    private MiScenarioGroup findGroup(Long scenarioGroupId) {
        return miScenarioGroupRepository.findByScenarioGroupIdAndIsDeletedFalse(scenarioGroupId)
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Scenario group not found. scenarioGroupId: %d".formatted(scenarioGroupId)));
    }

    private List<MiScenario> findGroupScenarios(Long scenarioGroupId) {
        return miScenarioRepository.findByScenarioGroupScenarioGroupIdAndIsDeletedFalseOrderByDisplayOrderAscScenarioIdAsc(scenarioGroupId);
    }

}
