package com.sk.domain.mi.service.nextgen.query;

import com.sk.domain.mi.dto.response.nextgen.ModelExecutionInputFileRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionInputFilesRes;
import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import com.sk.domain.shared.repository.master.tcm.UploadedFileRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelExecutionInputFilesProjector {

    private static final String FILE_RELATION_SCENARIO_CASE = "SCNRCASE";
    private static final List<String> REQUIRED_FILE_ROLES = List.of("DEMAND", "SUPPLY", "TRADE", "OUTPUT");

    private final UploadedFileRepository uploadedFileRepository;

    public ModelExecutionInputFilesRes project(Long scenarioId, Long caseId) {
        List<UploadedFile> uploadedFiles = uploadedFileRepository.findActiveRelationshipFilesOrderByFileNoAsc(
            Math.toIntExact(caseId),
            FILE_RELATION_SCENARIO_CASE
        );

        Map<String, List<UploadedFile>> filesByRole = new LinkedHashMap<>();
        REQUIRED_FILE_ROLES.forEach(role -> filesByRole.put(role, new ArrayList<>()));
        List<Long> invalidRoleFileNos = new ArrayList<>();

        for (UploadedFile uploadedFile : uploadedFiles) {
            List<String> matchedRoles = findFileRoles(uploadedFile.getOriginalFileNm());
            if (matchedRoles.size() != 1) {
                invalidRoleFileNos.add(uploadedFile.getFileNo());
                continue;
            }
            filesByRole.get(matchedRoles.get(0)).add(uploadedFile);
        }

        List<String> missingFileRoles = REQUIRED_FILE_ROLES.stream()
            .filter(role -> filesByRole.get(role).isEmpty())
            .toList();
        List<String> duplicateFileRoles = REQUIRED_FILE_ROLES.stream()
            .filter(role -> filesByRole.get(role).size() > 1)
            .toList();
        boolean complete = missingFileRoles.isEmpty()
            && duplicateFileRoles.isEmpty()
            && invalidRoleFileNos.isEmpty();

        if (!complete) {
            log.warn(
                "Model execution input files are incomplete. scenarioId={}, caseId={}, missingRoles={}, duplicateRoles={}, invalidRoleFileNos={}",
                scenarioId,
                caseId,
                missingFileRoles,
                duplicateFileRoles,
                invalidRoleFileNos
            );
        }

        List<ModelExecutionInputFileRes> files = REQUIRED_FILE_ROLES.stream()
            .flatMap(role -> filesByRole.get(role).stream().map(file -> new ModelExecutionInputFileRes(
                role,
                file.getFileNo(),
                file.getOriginalFileNm(),
                file.getFileSize()
            )))
            .toList();

        return new ModelExecutionInputFilesRes(complete, missingFileRoles, files);
    }

    private List<String> findFileRoles(String fileName) {
        String normalizedFileName = Optional.ofNullable(fileName).orElse("").toUpperCase(Locale.ROOT);
        return REQUIRED_FILE_ROLES.stream()
            .filter(normalizedFileName::contains)
            .toList();
    }
}
