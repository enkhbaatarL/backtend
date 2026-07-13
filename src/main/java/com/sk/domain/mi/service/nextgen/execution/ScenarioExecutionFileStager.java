package com.sk.domain.mi.service.nextgen.execution;

import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.mi.service.nextgen.execution.StagedScenarioExecutionFiles.StagedFile;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.FileService;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioExecutionFileStager {

    public static final String FINAL_RELATIONSHIP_CODE = "SCNRCASE";

    private final FileService fileService;
    private final AuthorizationService authorizationService;

    public StagedScenarioExecutionFiles stageSequentially(List<MultipartFile> files) {
        String companyCd = authorizationService.getCompanyCdWithPriority();
        Role role = authorizationService.getCurrentRole();
        List<StagedFile> stagedFiles = new ArrayList<>();

        try {
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                FileUploadRes uploadedFile = fileService.uploadStagedFileToRelationshipPath(
                    companyCd,
                    role,
                    file,
                    false,
                    FINAL_RELATIONSHIP_CODE
                );
                stagedFiles.add(new StagedFile(
                    uploadedFile.fileNo(),
                    index + 1,
                    uploadedFile.fileNm(),
                    file.getContentType(),
                    uploadedFile.fileSize()
                ));
            }
            return new StagedScenarioExecutionFiles(stagedFiles);
        } catch (RuntimeException failure) {
            discard(new StagedScenarioExecutionFiles(stagedFiles));
            throw failure;
        }
    }

    public void discard(StagedScenarioExecutionFiles stagedFiles) {
        if (stagedFiles == null) {
            return;
        }
        for (StagedFile stagedFile : stagedFiles.files()) {
            try {
                fileService.deleteStagedFile(stagedFile.fileNo(), FINAL_RELATIONSHIP_CODE);
            } catch (RuntimeException cleanupFailure) {
                log.error("Failed to discard staged scenario execution file. fileNo={}", stagedFile.fileNo(), cleanupFailure);
            }
        }
    }
}
