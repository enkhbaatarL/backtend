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
import com.sk.domain.mi.dto.response.nextgen.GroupModelParamProposalsRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchive;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchiver;
import com.sk.domain.mi.service.nextgen.execution.GroupModelInputFilesArchiver.ScenarioInputFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamCompareMerger;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamCompareMerger.CompareSource;
import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final MiScenarioGroupRepository miScenarioGroupRepository;
    private final MiScenarioRepository miScenarioRepository;
    private final ModelParamProposalService modelParamProposalService;
    private final ModelParamCompareMerger modelParamCompareMerger;
    private final ScenarioModelExecutionService scenarioModelExecutionService;
    private final GroupModelInputFilesArchiver groupModelInputFilesArchiver;

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

        return GroupModelParamProposalsRes.of(group, items);
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
