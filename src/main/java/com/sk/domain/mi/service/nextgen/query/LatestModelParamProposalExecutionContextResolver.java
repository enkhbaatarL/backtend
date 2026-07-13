package com.sk.domain.mi.service.nextgen.query;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LatestModelParamProposalExecutionContextResolver {

    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;

    public LatestModelParamProposalExecutionContext resolve(Long scenarioId) {
        ScenarioCase latestCase = scenarioCaseRepository
            .findFirstByScenarioIdAndExecTypeAndIsDeletedFalseOrderByCaseIdDesc(
                scenarioId,
                FundamentalModelExecType.SCENARIO
            )
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "latest scenario model execution not found. scenarioId: %d".formatted(scenarioId)
            ));

        ModelExecutionPreparation preparation = modelExecutionPreparationRepository
            .findByCaseIdAndIsDeletedFalse(latestCase.getCaseId())
            .orElseThrow(() -> latestCaseInvariantViolation(
                scenarioId,
                latestCase.getCaseId(),
                "non-deleted execution preparation is missing"
            ));

        if (preparation.getAgentProposalId() == null) {
            throw latestCaseInvariantViolation(
                scenarioId,
                latestCase.getCaseId(),
                "agent proposal id is missing"
            );
        }

        return new LatestModelParamProposalExecutionContext(
            scenarioId,
            latestCase.getCaseId(),
            preparation.getExecPrepId(),
            preparation.getAgentProposalId()
        );
    }

    private AppErrorException latestCaseInvariantViolation(Long scenarioId, Long caseId, String reason) {
        log.warn(
            "Latest model parameter proposal execution context invariant violation. scenarioId={}, caseId={}, reason={}",
            scenarioId,
            caseId,
            reason
        );
        return AppErrorException.of(
            ResultCode.Error.RSC_NOT_FOUND,
            "latest scenario model execution linkage not found. scenarioId: %d, caseId: %d"
                .formatted(scenarioId, caseId)
        );
    }

    public record LatestModelParamProposalExecutionContext(
        Long scenarioId,
        Long caseId,
        Long execPrepId,
        Long modelParamProposalId
    ) {
    }
}
