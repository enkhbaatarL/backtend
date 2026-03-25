package com.sk.domain.mi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.newsletter.NewsletterMailSendReq;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Profile("!dedicated")
@Tag(name = "13-5. Newsletter Mail API", description = "뉴스레터 Mail API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/newsletter")
@RequiredArgsConstructor
public class MiNewsletterMailController {

    private final MiNewsletterMailService miNewsletterMailService;
    private final ObjectMapper objectMapper;

    @ApiOrder(1)
    @Parameter(name = "newsletterId", description = "뉴스레터 마스터 ID")
    @Parameter(name = "issueNo", description = "회차 번호")
    @Operation(summary = "편집본 메일 발송",
            description = "리비전(편집본) 저장본 메일을 발송합니다. scheduledAt 미입력 또는 과거 시각은 즉시 발송으로 처리하고, 미래 시각은 현재 API에서 미지원(검증 에러)합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/{newsletterId}/{issueNo}/send-mail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<String> sendRevisionIssueMail(
        @PathVariable Integer newsletterId,
        @PathVariable Integer issueNo,
        @RequestPart(value = "request", required = false) String requestJson,
        @ModelAttribute NewsletterMailSendReq formRequest,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @UserResolve User actorUser) {
        NewsletterMailSendReq request = resolveMailSendReq(requestJson, formRequest);

        miNewsletterMailService.sendRevisionMail(
                newsletterId,
                issueNo,
                request.getSubject(),
                request.getHtmlBody(),
                images,
                request.getRecipientEmails(),
                request.getCcEmails(),
                request.getBccEmails(),
                request.getScheduledAt(),
                actorUser);

        return BaseResponseFactory.create("success");
    }

    private NewsletterMailSendReq resolveMailSendReq(String requestJson, NewsletterMailSendReq formRequest) {
        if (StringUtils.isBlank(requestJson)) {
            return formRequest;
        }

        NewsletterMailSendReq requestPart = parseRequestJson(requestJson);
        NewsletterMailSendReq mergedRequest = new NewsletterMailSendReq();
        mergedRequest.setSubject(StringUtils.defaultIfBlank(formRequest.getSubject(), requestPart.getSubject()));
        mergedRequest.setHtmlBody(StringUtils.defaultIfBlank(formRequest.getHtmlBody(), requestPart.getHtmlBody()));
        mergedRequest.setRecipientEmails(formRequest.getRecipientEmails() != null ? formRequest.getRecipientEmails() : requestPart.getRecipientEmails());
        mergedRequest.setCcEmails(formRequest.getCcEmails() != null ? formRequest.getCcEmails() : requestPart.getCcEmails());
        mergedRequest.setBccEmails(formRequest.getBccEmails() != null ? formRequest.getBccEmails() : requestPart.getBccEmails());
        mergedRequest.setScheduledAt(StringUtils.defaultIfBlank(formRequest.getScheduledAt(), requestPart.getScheduledAt()));
        return mergedRequest;
    }

    private NewsletterMailSendReq parseRequestJson(String requestJson) {
        try {
            return objectMapper.readValue(requestJson, NewsletterMailSendReq.class);
        } catch (JsonProcessingException e) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "request part must be valid JSON");
        }
    }
}
