package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.mi.service.fundamentalmodel.KubernetesJobService.ManagedActiveJob;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmd.projection.ModelExecutionRunStateRow;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelExecutionJobReconciler {

    private static final List<String> RECONCILABLE_FAILURE_REASONS = List.of("TIMEOUT", "SCENARIO_CANCELLED");

    private final KubernetesJobService kubernetesJobService;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;

    public void reconcile() {
        List<ManagedActiveJob> activeJobs = kubernetesJobService.findManagedActiveJobs();
        if (activeJobs.isEmpty()) {
            return;
        }

        Map<String, ModelExecutionRunStateRow> statesByRunId = scenarioCaseRunHistoryRepository
            .findRunStatesIncludingDeleted(activeJobs.stream().map(ManagedActiveJob::runId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(ModelExecutionRunStateRow::getRunId, Function.identity()));

        for (ManagedActiveJob job : activeJobs) {
            ModelExecutionRunStateRow state = statesByRunId.get(job.runId());
            if (shouldDelete(state)) {
                kubernetesJobService.deleteJob(job.runId());
                log.info("Deleted active model execution Job after backend-forced terminal transition. jobName={}, runId={}, failureReason={}",
                    job.jobName(), job.runId(), state.getFailureReasonCode());
            }
        }
    }

    private boolean shouldDelete(ModelExecutionRunStateRow state) {
        return state != null
            && state.getStatus() == FundamentalModelRunStatus.FAILED
            && RECONCILABLE_FAILURE_REASONS.contains(state.getFailureReasonCode());
    }
}
