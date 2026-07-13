package com.sk.domain.mi.service.nextgen.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "features.scheduling.model-execution-staged-file-cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ScenarioExecutionStagedFileCleanupScheduler {

    private final ScenarioExecutionStagedFileCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${features.scheduling.model-execution-staged-file-cleanup.fixed-delay-ms:3600000}")
    public void cleanupExpiredFiles() {
        try {
            cleanupService.cleanupExpired();
        } catch (RuntimeException e) {
            log.error("Scenario execution staged file cleanup scheduler failed.", e);
        }
    }
}
