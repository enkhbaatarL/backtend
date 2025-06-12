package com.sk.domain.shared.client.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AXMetaUploadRes(
       @JsonProperty("data")
        List<Data> data
    ){

        public record Data(
            @JsonProperty("file_name")
            String fileName,

            @JsonProperty("temp_file_path")
            String tempFilePath,

            @JsonProperty("file_meta_data")
            String fileMetaData,

            @JsonProperty("knowledge_config")
            String knowledgeConfig
        ) {

        } 
}
// @formatter:on