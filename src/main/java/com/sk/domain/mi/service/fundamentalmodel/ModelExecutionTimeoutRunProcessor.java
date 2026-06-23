package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelRunStatus;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelExecutionTimeoutRunProcessor {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String TIMEOUT_FAILURE_REASON = "TIMEOUT";
    private static final String TIMEOUT_ERROR_MESSAGE = "Model execution timed out after 10 minutes.";

    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTimedOutRun(String runId) {
        LocalDateTime timedOutAt = scenarioCaseRunHistoryRepository.findCurrentDatabaseTimeByRunId(runId)
            .orElseThrow(() -> new IllegalStateException("timed-out model run not found. runId=%s".formatted(runId)));
        int updateCount = scenarioCaseRunHistoryRepository.updateRunningToTerminal(
            runId,
            FundamentalModelRunStatus.FAILED,
            FundamentalModelRunStatus.RUNNING,
            timedOutAt,
            TIMEOUT_FAILURE_REASON,
            TIMEOUT_ERROR_MESSAGE,
            timedOutAt,
            SYSTEM_ACTOR
        );
        if (updateCount == 0) {
            log.info("Skip timed-out model run because it is no longer RUNNING. runId={}", runId);
            return;
        }

        scenarioCaseRepository.updateLatestRunStatus(
            runId,
            FundamentalModelCaseStatus.FAILED,
            timedOutAt,
            SYSTEM_ACTOR
        );
    }
}
