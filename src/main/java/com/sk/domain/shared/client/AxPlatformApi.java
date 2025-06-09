package com.sk.domain.shared.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import com.sk.domain.shared.client.dto.request.AXMetaFileInfoRegReq;
import com.sk.domain.shared.client.dto.response.AXMetaFileUploadRes;

@HttpExchange
public interface AxPlatformApi {

    @PostExchange(value = "/api/v1/datasource/upload/files", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    AXMetaFileUploadRes postMessage(@RequestPart(value = "files") MultipartFile file, @RequestHeader("Authorization") String authToken);

    @PutExchange("/api/v1/datasource/{datasourceId}")
    AXMetaFileUploadRes putMessage(@PathVariable String datasourceId, @RequestBody AXMetaFileInfoRegReq req);

}
