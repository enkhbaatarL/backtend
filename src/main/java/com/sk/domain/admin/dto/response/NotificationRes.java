package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record NotificationRes(

        @Schema(description = "이메일 발송 일련번호", example = "1")
        Integer emailSendSn,

        @Schema(description = "이메일 수신 사용자 로그인 ID", example = "test@test.com")
        String emailReceiveUserLoginId,

        @Schema(description = "이메일 제목", example = "테스트 이메일")
        String emailTitleNm,

        @Schema(description = "이메일 내용", example = "테스트 이메일 내용")
        String emailCntt,

        @Schema(description = "이메일 발송 상태", example = "발송완료")
        String emailSendStatusNm,

        @Schema(description = "이메일 발송 오류 내용", example = "테스트 이메일 오류 내용")
        String emailSendErrorCntt,

        @Schema(description = "이메일 발송 일시", example = "2025-01-01 00:00:00")
        LocalDateTime emailSendDtm,

        @Schema(description = "이메일 수신 사용자명", example = "홍길동")
        String userNm,

        @Schema(description = "사용자역할코드", example = "ADMIN")
        UserRoleCd userRoleCd,

        @Schema(description = "직책명", example = "팀장")
        String onDutyNm,

        @Schema(description = "부서명", example = "개발팀")
        String departmentNm,

        @Schema(description = "고객사번호", example = "1")
        Integer customerCompanyNo,

        @Schema(description = "고객사명", example = "테스트 고객사")
        String customerCompanyNm,

        @JsonUnwrapped
        @Schema(description = "감사 정보")
        AuditInfo auditInfo

) {

    @QueryProjection
    public NotificationRes(Integer emailSendSn, String emailReceiveUserLoginId, String emailTitleNm, String emailCntt,
            String emailSendStatusNm, String emailSendErrorCntt, LocalDateTime emailSendDtm, String userNm,
            UserRoleCd userRoleCd, String onDutyNm, String departmentNm, Integer customerCompanyNo,
            String customerCompanyNm, String createUserId, LocalDateTime createDt, String modifyUserId,
            LocalDateTime modifyDt) {
        this(emailSendSn, emailReceiveUserLoginId, emailTitleNm, emailCntt, emailSendStatusNm, emailSendErrorCntt,
                emailSendDtm, userNm, userRoleCd, onDutyNm, departmentNm, customerCompanyNo, customerCompanyNm,
                AuditInfo.of(createUserId, createDt, modifyUserId, modifyDt));
    }

    @Builder
    public NotificationRes(Integer emailSendSn, String emailReceiveUserLoginId, String emailTitleNm, String emailCntt,
            String emailSendStatusNm, String emailSendErrorCntt, LocalDateTime emailSendDtm, String userNm,
            UserRoleCd userRoleCd, String onDutyNm, String departmentNm, Integer customerCompanyNo,
            String customerCompanyNm, AuditInfo auditInfo) {
        this.emailSendSn = emailSendSn;
        this.emailReceiveUserLoginId = emailReceiveUserLoginId;
        this.emailTitleNm = emailTitleNm;
        this.emailCntt = emailCntt;
        this.emailSendStatusNm = emailSendStatusNm;
        this.emailSendErrorCntt = emailSendErrorCntt;
        this.emailSendDtm = emailSendDtm;
        this.userNm = userNm;
        this.userRoleCd = userRoleCd;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.auditInfo = auditInfo;
    }
}
