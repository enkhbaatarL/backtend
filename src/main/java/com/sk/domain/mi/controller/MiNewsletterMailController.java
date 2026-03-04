package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.service.MiNewsletterMailService;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "13. Newsletter Mail API", description = "뉴스레터 Mail API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterMailController {

    private final MiNewsletterMailService miNewsletterMailService;

    @ApiOrder(1)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Parameter(name = "issueNo", description = "회차 번호")
    @Operation(summary = "오리지널 메일 발송", description = "오리지널 회차 메일을 발송합니다. recipientEmails 미입력 시 구독자를 대상으로 발송합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/{newsletterId}/{issueNo}/send-mail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<String> sendOriginalIssueMail(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
            @RequestParam("subject") String subject,
            @RequestParam("htmlBody") String htmlBody,
            @RequestParam(value = "recipientEmails", required = false) List<String> recipientEmails,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "cids", required = false) List<String> cids,
            @UserResolve User actorUser) {

        miNewsletterMailService.sendOriginalIssueMail(
                newsletterId,
                issueNo,
                subject,
                htmlBody,
                images,
                cids,
                recipientEmails,
                actorUser);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(2)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Parameter(name = "issueNo", description = "회차 번호")
    @Operation(summary = "에디션 메일 발송", description = "에디션 저장본 메일을 발송합니다. recipientEmails 미입력 시 요청 사용자 이메일로 발송합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/{newsletterId}/{issueNo}/edition/send-mail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<String> sendEditionMail(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
            @RequestParam("subject") String subject,
            @RequestParam("htmlBody") String htmlBody,
            @RequestParam(value = "recipientEmails", required = false) List<String> recipientEmails,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "cids", required = false) List<String> cids,
            @UserResolve User actorUser) {

        miNewsletterMailService.sendEditionMail(
                newsletterId,
                issueNo,
                subject,
                htmlBody,
                images,
                cids,
                recipientEmails,
                actorUser);

        return BaseResponseFactory.create("success");
    }
}
