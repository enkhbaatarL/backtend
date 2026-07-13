package com.sk.domain.mi.service.nextgen.execution;

import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelJobArgs;
import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioModelJobDispatcher {

    private static final String CALLBACK_ACTOR = "SYSTEM";
    private static final String JOB_SUBMIT_FAILURE_REASON = "JOB_SUBMIT_FAILED";
    private static final String JOB_SUBMIT_FAILURE_MESSAGE = "Model execution dispatch failed.";

    private final KubernetesJobService kubernetesJobService;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final PlatformTransactionManager transactionManager;

    public void dispatchAfterCommit(
        Long caseId,
        String runId,
        FundamentalModelJobArgs jobArgs,
        Long execPrepId
    ) {
        Runnable submitJob = () -> dispatch(caseId, runId, jobArgs, execPrepId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitJob.run();
                }
            });
            return;
        }
        submitJob.run();
    }

    void dispatch(Long caseId, String runId, FundamentalModelJobArgs jobArgs, Long execPrepId) {
        try {
            kubernetesJobService.createFundamentalModelJob(jobArgs)
                .whenComplete((ignored, ex) -> {
                    if (ex != null) {
                        log.warn(
                            "Kubernetes scenario model job creation failed: scenarioId={}, runId={}, caseId={}, execPrepId={}",
                            jobArgs.getScenarioId(),
                            runId,
                            caseId,
                            execPrepId,
                            ex
                        );
                        markRunFailed(runId, caseId, execPrepId);
                    }
                });
        } catch (RuntimeException e) {
            log.warn(
                "Kubernetes scenario model job creation failed: scenarioId={}, runId={}, caseId={}, execPrepId={}",
                jobArgs.getScenarioId(),
                runId,
                caseId,
                execPrepId,
                e
            );
            markRunFailed(runId, caseId, execPrepId);
        }
    }

    private void markRunFailed(String runId, Long caseId, Long execPrepId) {
        try {
            TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
            requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            requiresNew.executeWithoutResult((TransactionStatus ignored) -> {
                LocalDateTime failedAt = LocalDateTime.now();
                int updateCount = scenarioCaseRunHistoryRepository.updateRunningToTerminal(
                    runId,
                    FundamentalModelRunStatus.FAILED,
                    FundamentalModelRunStatus.RUNNING,
                    failedAt,
                    JOB_SUBMIT_FAILURE_REASON,
                    JOB_SUBMIT_FAILURE_MESSAGE,
                    failedAt,
                    CALLBACK_ACTOR
                );
                if (updateCount > 0) {
                    scenarioCaseRepository.updateLatestRunStatus(
                        runId,
                        FundamentalModelCaseStatus.FAILED,
                        failedAt,
                        CALLBACK_ACTOR
                    );
                    markPreparationFailed(execPrepId, failedAt);
                } else {
                    log.info("Skip marking non-running scenario model run as failed. runId={}, caseId={}", runId, caseId);
                }
            });
        } catch (RuntimeException failureUpdateException) {
            log.error(
                "Failed to persist scenario model dispatch failure. runId={}, caseId={}, execPrepId={}",
                runId,
                caseId,
                execPrepId,
                failureUpdateException
            );
        }
    }

    private void markPreparationFailed(Long execPrepId, LocalDateTime failedAt) {
        if (execPrepId == null) {
            return;
        }
        int preparationUpdated = modelExecutionPreparationRepository.transitionStatus(
            execPrepId,
            List.of(ModelExecutionPreparationStatus.EXECUTING),
            ModelExecutionPreparationStatus.EXECUTION_FAILED,
            JOB_SUBMIT_FAILURE_REASON,
            CALLBACK_ACTOR,
            failedAt
        );
        if (preparationUpdated == 0) {
            log.info("Skip marking non-executing preparation as failed. execPrepId={}", execPrepId);
        }
    }
}
