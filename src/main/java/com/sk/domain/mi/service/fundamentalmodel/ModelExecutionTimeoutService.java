package com.sk.domain.mi.service.fundamentalmodel;

import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelExecutionTimeoutService {

    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ModelExecutionTimeoutRunProcessor modelExecutionTimeoutRunProcessor;

    public void processTimedOutRuns() {
        List<String> runIds = scenarioCaseRunHistoryRepository.findTimedOutRunningRunIds();
        for (String runId : runIds) {
            try {
                modelExecutionTimeoutRunProcessor.processTimedOutRun(runId);
            } catch (RuntimeException e) {
                log.error("Timed-out model run processing failed. runId={}", runId, e);
            }
        }
    }
}
