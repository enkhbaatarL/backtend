package com.sk.domain.admin.controller;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.request.client.ClientAccessInfoReq;
import com.sk.domain.admin.dto.response.client.ClientAccessInfoRes;
import com.sk.domain.admin.service.ClientAccessInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "30. 클라이언트 접근권한 API", description = "클라이언트 접근권한 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClientAccessInfoController {

    private final ClientAccessInfoService clientAccessInfoService;

    @Operation(summary = "클라이언트 접근권한 목록 조회 API", description = "클라이언트 접근권한 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/client-access-infos")
    public BaseResponse<List<ClientAccessInfoRes>> getClientAccessInfos() {
        return BaseResponseFactory.create(clientAccessInfoService.getClientAccessInfos());
    }

    @Operation(summary = "클라이언트 접근권한 등록 API", description = "클라이언트 접근권한을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/client-access-info")
    public BaseResponse<ClientAccessInfoRes> postClientAccessInfo(
        @RequestBody @Valid ClientAccessInfoReq.Insert dto
    ) {
        return BaseResponseFactory.create(clientAccessInfoService.postClientAccessInfo(dto));
    }

    @Operation(summary = "클라이언트 접근권한 수정 API", description = "클라이언트 접근권한을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/client-access-info")
    public BaseResponse<ClientAccessInfoRes> patchClientAccessInfo(
        @RequestBody @Valid ClientAccessInfoReq.Update dto
    ) {
        return BaseResponseFactory.create(clientAccessInfoService.patchClientAccessInfo(dto));
    }

    @Operation(summary = "클라이언트 접근권한 삭제 API", description = "클라이언트 접근권한을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/client-access-info/{clientAccessRegNo}")
    public BaseResponse<ClientAccessInfoRes> postClientAccessInfo(
        @PathVariable @Valid Integer clientAccessRegNo
    ) {
        return BaseResponseFactory.create(clientAccessInfoService.deleteClientAccessInfo(clientAccessRegNo));
    }

}
