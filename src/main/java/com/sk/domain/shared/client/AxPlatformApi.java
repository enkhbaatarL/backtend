package com.sk.domain.shared.client;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import com.sk.domain.shared.client.dto.request.AXMetaRegisterReq;
import com.sk.domain.shared.client.dto.response.AXMetaLookUpRes;
import com.sk.domain.shared.client.dto.response.AXMetaRegisterRes;
import com.sk.domain.shared.client.dto.response.AXMetaUploadRes;

@HttpExchange
public interface AxPlatformApi {

    @PostExchange(value = "/api/v1/datasources/upload/files", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    AXMetaUploadRes upload(@RequestHeader("Authorization") String authToken, @RequestPart(value = "files") MultipartFile file);

    @PutExchange("/api/v1/datasources/{datasourceId}")
    AXMetaRegisterRes register(@RequestHeader("Authorization") String authToken, @PathVariable String datasourceId,
            @RequestBody AXMetaRegisterReq req);

    @GetExchange("/api/v1/datasources/{datasourceId}/files")
    AXMetaLookUpRes lookup(@RequestHeader("Authorization") String authToken, @PathVariable String datasourceId,
            @RequestParam(name = "file_name") String fileName);
}
