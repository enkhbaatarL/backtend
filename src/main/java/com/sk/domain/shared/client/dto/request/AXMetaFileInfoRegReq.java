package com.sk.domain.shared.client.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AXMetaFileInfoRegReq(
       @JsonProperty("modified_files")
        List<ModifiedFile> modifiedFiles
    ){

        public record ModifiedFile(
            @JsonProperty("file_name")
            String fileName,

            @JsonProperty("temp_file_path")
            String tempFilePath,

            @JsonProperty("file_meta_data")
            String fileMetaData,

            @JsonProperty("knowledge_config")
            String knowledgeConfig,

            @JsonProperty("status")
            String status
        ) {
            public ModifiedFile(String fileName, String tempFilePath, String fileMetaData, String knowledgeConfig, String status) {
                this.fileName = fileName;
                this.tempFilePath = tempFilePath;
                this.fileMetaData = fileMetaData;
                this.knowledgeConfig = knowledgeConfig;
                this.status = status;
            }
        } 
}
// @formatter:on