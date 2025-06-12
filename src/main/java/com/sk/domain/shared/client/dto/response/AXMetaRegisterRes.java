package com.sk.domain.shared.client.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

        @JsonProperty("is_deleted")    
        boolean isDeleted,

        @JsonProperty("scope")         
        String scope,

        @JsonProperty("id")            
        String id,

        @JsonProperty("created_at")    
        Instant createdAt,

        @JsonProperty("updated_at")    
        Instant updatedAt,

        @JsonProperty("status")        
        String status,

        @JsonProperty("bucket_name")   
        String bucketName,

        @JsonProperty("files")         
        List<FileDetail> files
    ){

        public record FileDetail(
            @JsonProperty("id")              
            String id,

            @JsonProperty("datasource_id")   
            String datasourceId,

            @JsonProperty("file_name")       
            String fileName,

            @JsonProperty("file_path")       
            String filePath,

            @JsonProperty("file_size")       
            long fileSize,

            @JsonProperty("is_deleted")      
            boolean isDeleted,

            @JsonProperty("created_at")      
            Instant createdAt,

            @JsonProperty("updated_at")      
            Instant updatedAt,

            @JsonProperty("created_by")      
            String createdBy,

            @JsonProperty("updated_by")      
            String updatedBy,

            @JsonProperty("s3_etag")         
            String s3Etag,

            @JsonProperty("file_metadata")   
            Map<String, Object> fileMetadata,

            @JsonProperty("knowledge_config") 
            Map<String, Object> knowledgeConfig
        ) {
            
        } 
}
// @formatter:on