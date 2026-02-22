package com.sk.domain.shared.client.dto.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
            ObjectNode fileMetaData,

            @JsonProperty("knowledge_config")
            ObjectNode knowledgeConfig
        ) {

        }
}
// @formatter:on
