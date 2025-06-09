package com.sk.domain.shared.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AXMetaFileInfoRegReq(
       @JsonProperty("modified_files")
        Data[] modifiedFiles
    ){

        public record Data(
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
            
        } 
}
// @formatter:on