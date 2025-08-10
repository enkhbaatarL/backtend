package com.sk.domain.shared.client.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
public record AXMetaRegisterRes(
        @JsonProperty("project_id")    
        String projectId,

        @JsonProperty("name")          
        String name,

        @JsonProperty("type")          
        String type,

        @JsonProperty("created_by")    
        String createdBy,

        @JsonProperty("updated_by")    
        String updatedBy,

        @JsonProperty("description")   
        String description,

        @JsonProperty("s3_config")   
        String s3Config,

        @JsonProperty("is_deleted")    
        boolean isDeleted,

        @JsonProperty("scope")         
        String scope,

        @JsonProperty("id")            
        String id,

        @JsonProperty("created_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        LocalDateTime updatedAt,

        @JsonProperty("status")        
        String status,

        @JsonProperty("bucket_name")   
        String bucketName,

        @JsonProperty("files")         
        List<FileDetail> files
    ){

        public record FileDetail(
            @JsonProperty("datasource_id")
            String datasourceId,

            @JsonProperty("file_name")
            String fileName,

            @JsonProperty("file_path")
            String filePath,

            @JsonProperty("file_size")
            long fileSize,

            @JsonProperty("file_metadata")
            String fileMetadata,

            @JsonProperty("knowledge_config")
            String knowledgeConfig,

            @JsonProperty("created_by")
            String createdBy,

            @JsonProperty("updated_by")
            String updatedBy,

            @JsonProperty("is_deleted")
            boolean isDeleted,

            @JsonProperty("id")
            String id,

            @JsonProperty("created_at")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            LocalDateTime createdAt,

            @JsonProperty("updated_at")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            LocalDateTime updatedAt,

            @JsonProperty("s3_etag")
            String s3Etag
        ) {
            
        } 
}
// @formatter:on