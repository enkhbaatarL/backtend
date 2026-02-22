package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AXMetaRegisterReq {

    @JsonProperty("modified_files")
    private List<ModifiedFile> modifiedFiles = new ArrayList<>();

    public static class ModifiedFile {
        @JsonProperty("file_name")
        String fileName;

        @JsonProperty("file_id")
        String fileId;

        @JsonProperty("temp_file_path")
        String tempFilePath;

        @JsonProperty("file_metadata")
        ObjectNode fileMetaData;

        @JsonProperty("knowledge_config")
        ObjectNode knowledgeConfig;

        @JsonProperty("status")
        String status;

        public ModifiedFile(String fileName, String fileId, String tempFilePath, ObjectNode fileMetaData, ObjectNode knowledgeConfig, String status) {
            this.fileName = fileName;
            this.fileId = fileId;
            this.tempFilePath = tempFilePath;
            this.fileMetaData = fileMetaData;
            this.knowledgeConfig = knowledgeConfig;
            this.status = status;
        }
    }

    public AXMetaRegisterReq() {
    }

    public AXMetaRegisterReq(List<ModifiedFile> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    public void addModifiedFile(ModifiedFile modifiedFile) {
        this.modifiedFiles.add(modifiedFile);
    }

    // 게터·세터 필요 시 Lombok @Data 등 활용
}
