package com.sk.domain.mi.service.nextgen.execution;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.service.FileService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioExecutionStagedFileCleanupService {

    private final FileService fileService;

    @Value("${features.scheduling.model-execution-staged-file-cleanup.ttl-hours:24}")
    private long ttlHours;

    @Value("${features.scheduling.model-execution-staged-file-cleanup.batch-size:100}")
    private int batchSize;

    public int cleanupExpired() {
        LocalDateTime createdBefore = LocalDateTime.now().minusHours(ttlHours);
        List<UploadedFile> expiredFiles = fileService.findExpiredStagedFiles(
            ScenarioExecutionFileStager.FINAL_RELATIONSHIP_CODE,
            createdBefore,
            batchSize
        );

        int deletedCount = 0;
        for (UploadedFile expiredFile : expiredFiles) {
            try {
                if (fileService.deleteStagedFile(
                    expiredFile.getFileNo(),
                    ScenarioExecutionFileStager.FINAL_RELATIONSHIP_CODE
                )) {
                    deletedCount++;
                }
            } catch (RuntimeException e) {
                log.error("Failed to clean expired staged scenario execution file. fileNo={}", expiredFile.getFileNo(), e);
            }
        }
        if (deletedCount > 0) {
            log.info("Expired staged scenario execution files cleaned. count={}", deletedCount);
        }
        return deletedCount;
    }
}
