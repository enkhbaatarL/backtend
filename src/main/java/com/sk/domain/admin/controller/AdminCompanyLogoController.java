package com.sk.domain.admin.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.MiCompanyLogoRes;
import com.sk.domain.mi.service.MiCompanyLogoService;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고객회사 로고 API.
 */
@Profile("!dedicated")
@AccessIpCheck
@Tag(name = "07. 고객사 로고 API", description = "고객사 로고 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/company-management")
@RequiredArgsConstructor
public class AdminCompanyLogoController {

    private final MiCompanyLogoService miCompanyLogoService;

    @ApiOrder(1)
    @Operation(
        summary = "회사 로고 조회 API",
        description = """
            현재 컨텍스트 회사에 등록된 로고 정보를 조회합니다. <br>
            로고 관리 화면에서 현재 등록 상태를 표시할 때 사용합니다. <br>
            **화면 헤더 렌더링용으로는 해당 API가 아니라 `고객사 상세 조회 API`(`GET /api/v1/mi/company-management/companies/{companyNo}/detail`)의 `logoUrl` 필드를 사용합니다.** <br>

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 조회 대상으로 확정된 고객회사번호
            - `logoUrl`: 로고 이미지 URL. **전용 로고가 없으면 `null`** 이며, 이때 화면은 FE 기본 로고를 사용합니다.

            <br><br>
            ### logoUrl 형식
            FE는 형태를 구분할 필요 없이 받은 문자열을 그대로 `img` 태그의 `src`에 넣으면 됩니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/companies/me/logo")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN),
        @PermissionRule(role = Role.OPERATOR)
    })
    public BaseResponse<MiCompanyLogoRes> getCompanyLogo() {
        return BaseResponseFactory.create(miCompanyLogoService.getLogo());
    }

    @ApiOrder(2)
    @Operation(
        summary = "회사 로고 등록/변경 API",
        description = """
            현재 컨텍스트 회사의 로고를 등록하거나 교체합니다. <br>
            **등록과 교체를 구분하지 않습니다.** 기존 로고 유무와 무관하게, 호출하면 요청한 파일이 현재 로고가 됩니다. <br>

            <br><br>
            ### 요청 형식
            `multipart/form-data`이며 파일 파트 이름은 `file`입니다.
            ```
            PUT /api/v1/admin/company-management/companies/me/logo
            Content-Type: multipart/form-data

            file: (binary)
            ```

            <br><br>
            ### 파일 제약
            - **형식**: PNG, JPEG만 허용합니다. (SVG, WEBP, GIF, BMP 불가)
            - **크기**: 기본 3MB 이하입니다.
            - **파일명**: 확장자가 있어야 합니다.

            형식 판별은 **파일명 확장자나 `Content-Type` 헤더가 아니라 파일 내용으로** 합니다. <br>
            둘 다 클라이언트가 임의로 보낼 수 있으므로, 실제로 디코딩 가능한 이미지인지를 검사합니다. <br>
            따라서 확장자만 `.png`로 바꾼 비이미지 파일은 거부됩니다. <br>
            SVG를 막는 이유는 스크립트 실행이 가능한 XML이라, 서비스 도메인에서 직접 열릴 경우 동일 출처 스크립트 실행 경로가 되기 때문입니다.

            <br><br>
            ### 교체 시 동작
            새 파일을 먼저 업로드한 뒤, **교체 이전 시점에 활성 상태였던 로고 파일을 모두 삭제**합니다. <br>
            따라서 호출 이후 해당 회사의 활성 로고는 항상 1건이며, 이전 로고 URL은 즉시 무효가 됩니다.

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 로고가 등록된 고객회사번호
            - `logoUrl`: 새로 등록된 로고의 URL. 이 값으로 화면을 즉시 갱신할 수 있습니다.

            <br><br>
            ### 오류
            아래 오류는 모두 HTTP `400`이며 응답 본문의 `code`로 구분합니다.
            | code | 상황 |
            |---|---|
            | `FILE-001` | 파일이 없거나, PNG/JPEG가 아니거나, 파일명에 확장자가 없음 |
            | `FILE-003` | 허용 크기 초과 |

            권한이 없으면 HTTP `403`입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "등록/교체 성공")
    @PutMapping(value = "/companies/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN),
        @PermissionRule(role = Role.OPERATOR)
    })
    public BaseResponse<MiCompanyLogoRes> changeCompanyLogo(
        @Parameter(description = "로고 이미지 파일 (PNG 또는 JPEG, 기본 1MB 이하)")
        @RequestPart(value = "file") MultipartFile file) {
        return BaseResponseFactory.create(miCompanyLogoService.changeLogo(file));
    }

    @ApiOrder(3)
    @Operation(
        summary = "회사 로고 해제 API",
        description = """
            현재 컨텍스트 회사의 전용 로고를 해제하여 FE 기본 로고를 사용하도록 되돌립니다. <br>
            잘못 등록한 로고를 되돌리는 용도입니다. <br>

            <br><br>
            ### 동작
            해당 회사의 활성 로고 파일을 모두 삭제 처리하고 오브젝트 스토리지에서도 제거합니다. <br>
            **멱등입니다.** 등록된 로고가 없는 상태에서 호출해도 오류 없이 성공합니다.

            <br><br>
            ### 응답 필드
            - `customerCompanyNo`: 대상 고객회사번호
            - `logoUrl`: 해제 후이므로 **항상 `null`** 입니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "해제 성공")
    @DeleteMapping("/companies/me/logo")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.ADMIN),
        @PermissionRule(role = Role.OPERATOR)
    })
    public BaseResponse<MiCompanyLogoRes> removeCompanyLogo() {
        return BaseResponseFactory.create(miCompanyLogoService.removeLogo());
    }

}
