package com.sk.domain.admin.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.client.ClientAccessInfoReq;
import com.sk.domain.admin.dto.response.client.ClientAccessInfoRes;
import com.sk.domain.admin.dto.response.client.ClientAccessSecretRes;
import com.sk.domain.admin.service.AdminSystemService;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "06. 시스템 관리 API", description = "시스템 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/system-management")
@RequiredArgsConstructor
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
    })
    @Operation(summary = "API 관리 목록 조회 API (with pagination)", description = "API 접속 정보 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/client-access")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<ClientAccessInfoRes>> getClientAccessInfos(
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size
    ) {
        return BaseResponseFactory.create(adminSystemService.getClientAccessInfos(page, size));
    }

    @ApiOrder(2)
    @Operation(summary = "API Key 등록 API", description = "API Key를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/client-access")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<ClientAccessInfoRes> postClientAccessInfo(
        @RequestBody @Valid ClientAccessInfoReq.Insert dto
    ) {
        return BaseResponseFactory.create(adminSystemService.postClientAccessInfo(dto));
    }

    @ApiOrder(3)
    @Parameters({
        @Parameter(name = "clientAccessRegNo", description = "클라이언트 접속 등록 번호"),
    })
    @Operation(summary = "API Client-Secret 재발급 API", description = "클라이언트 Client-Secret을 재발급합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/client-access/{clientAccessRegNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<ClientAccessInfoRes> patchClientAccessInfo(
        @PathVariable Integer clientAccessRegNo
    ) {
        return BaseResponseFactory.create(adminSystemService.patchClientAccessInfo(clientAccessRegNo));
    }

    @ApiOrder(4)
    @Parameters({
        @Parameter(name = "clientAccessRegNo", description = "클라이언트 접속 등록 번호"),
    })
    @Operation(summary = "API Key 삭제 API", description = "API Key를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/client-access/{clientAccessRegNo}")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<ClientAccessInfoRes> deleteClientAccessInfo(
        @PathVariable @Valid Integer clientAccessRegNo
    ) {
        return BaseResponseFactory.create(adminSystemService.deleteClientAccessInfo(clientAccessRegNo));
    }

    @ApiOrder(5)
    @Parameters({
        @Parameter(name = "clientAccessRegNo", description = "클라이언트 접속 등록 번호"),
    })
    @Operation(summary = "Client-Secret(복호화) 조회 API", description = "복호화된 Client-Secret을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/client-access/client-access/{clientAccessRegNo}/reveal-secret")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<ClientAccessSecretRes> getClientAccessInfoSecret(
        @PathVariable Integer clientAccessRegNo
    ) {
        return BaseResponseFactory.create(adminSystemService.getClientAccessInfoSecret(clientAccessRegNo));
    }

}
