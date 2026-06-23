package com.sk.domain.mi.service.fundamentalmodel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "features.scheduling.model-execution-timeout",
    name = "enabled",
    havingValue = "true"
)
public class ModelExecutionTimeoutScheduler {

    private final ModelExecutionTimeoutService modelExecutionTimeoutService;

    @Scheduled(fixedDelay = 30_000)
    public void processTimedOutRuns() {
        try {
            modelExecutionTimeoutService.processTimedOutRuns();
        } catch (RuntimeException e) {
            log.error("Model execution timeout scheduler failed.", e);
        }
    }
}
