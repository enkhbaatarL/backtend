package com.sk.domain.shared.client;

import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface AxPlatformApi {

    // @PostExchange("/api/v1/datasource/upload/files", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    // ModelAgentRes postMessage(@RequestPart(value = "files") MultipartFile file);

    // @PostExchange("/api/v1/datasource/{datasourceId}")
    // ModelAgentRes postMessage(@PathVariable long datasourceId, @RequestBody String req);

}
