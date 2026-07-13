package com.sk.domain.mi.service.nextgen.execution;

import com.sk.domain.mi.dto.response.nextgen.ModelExecutionFileRes;
import java.util.List;

public record StagedScenarioExecutionFiles(
    List<StagedFile> files
) {

    public StagedScenarioExecutionFiles {
        files = List.copyOf(files);
    }

    public List<Long> fileNos() {
        return files.stream().map(StagedFile::fileNo).toList();
    }

    public List<ModelExecutionFileRes> toResponseFiles() {
        return files.stream()
            .map(file -> new ModelExecutionFileRes(
                file.fileNo(),
                file.order(),
                file.fileName(),
                file.mimeType(),
                file.fileSize()
            ))
            .toList();
    }

    public record StagedFile(
        Long fileNo,
        int order,
        String fileName,
        String mimeType,
        Long fileSize
    ) {
    }
}
