package com.sk.domain.admin.dto.response.notification;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.enums.EmailSendStatusName;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record AdminNotificationRes(

        @Schema(description = "이메일 발송 일련번호", example = "1") Integer emailSendSn,

        @Schema(description = "이메일 수신 사용자 로그인 ID", example = "test@test.com") String emailReceiveUserLoginId,

        @Schema(description = "이메일 제목", example = "테스트 이메일") String emailTitleNm,

        @Schema(description = "이메일 내용", example = "테스트 이메일 내용") String emailCntt,

        @Schema(description = "이메일 발송 상태", example = "PENDING") EmailSendStatusName emailSendStatusNm,

        @Schema(description = "이메일 발송 오류 내용", example = "테스트 이메일 오류 내용") String emailSendErrorCntt,

        @Schema(description = "이메일 발송 일시", example = "2025-01-01 00:00:00") LocalDateTime emailSendDtm,

        @Schema(description = "이메일 발송 성공 여부", example = "true") Boolean isSendSuccess,

        @Schema(description = "이메일 수신 사용자명", example = "홍길동") String userNm,

        @Schema(description = "사용자역할코드", example = "ADMIN") UserRoleCd userRoleCd,

        @Schema(description = "직위명", example = "팀장") String positionNm,

        @Schema(description = "직급명", example = "사원") String jobGradeNm,

        @Schema(description = "직책명", example = "팀장") String onDutyNm,

        @Schema(description = "부서명", example = "개발팀") String departmentNm,

        @Schema(description = "고객사번호", example = "1") Integer customerCompanyNo,

        @Schema(description = "고객사명", example = "테스트 고객사") String customerCompanyNm,

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "상품데이터명") String goodsDataNm,

        @JsonUnwrapped @Schema(description = "감사 정보") AuditInfo auditInfo

) {

    @QueryProjection
    public AdminNotificationRes(Integer emailSendSn, String emailReceiveUserLoginId, String emailTitleNm, String emailCntt,
            EmailSendStatusName emailSendStatusNm, String emailSendErrorCntt, LocalDateTime emailSendDtm, Boolean isSendSuccess, String userNm,
            UserRoleCd userRoleCd, String positionNm, String jobGradeNm, String onDutyNm, String departmentNm,
            Integer customerCompanyNo, String customerCompanyNm, Integer goodsDataNo, String goodsDataNm, String createUserId, LocalDateTime createDt,
            String modifyUserId, LocalDateTime modifyDt) {
        this(emailSendSn, emailReceiveUserLoginId, emailTitleNm, emailCntt, emailSendStatusNm, emailSendErrorCntt,
                emailSendDtm, isSendSuccess, userNm, userRoleCd, positionNm, jobGradeNm, onDutyNm, departmentNm, customerCompanyNo,
                customerCompanyNm, goodsDataNo, goodsDataNm, AuditInfo.ofSimple(createUserId, createDt, modifyUserId, modifyDt));
    }

    @Builder
    public AdminNotificationRes(Integer emailSendSn, String emailReceiveUserLoginId, String emailTitleNm, String emailCntt,
            EmailSendStatusName emailSendStatusNm, String emailSendErrorCntt, LocalDateTime emailSendDtm, Boolean isSendSuccess, String userNm,
            UserRoleCd userRoleCd, String positionNm, String jobGradeNm, String onDutyNm, String departmentNm,
            Integer customerCompanyNo, String customerCompanyNm, Integer goodsDataNo, String goodsDataNm, AuditInfo auditInfo) {
        this.emailSendSn = emailSendSn;
        this.emailReceiveUserLoginId = emailReceiveUserLoginId;
        this.emailTitleNm = emailTitleNm;
        this.emailCntt = emailCntt;
        this.emailSendStatusNm = emailSendStatusNm;
        this.emailSendErrorCntt = emailSendErrorCntt;
        this.emailSendDtm = emailSendDtm;
        this.isSendSuccess = isSendSuccess;
        this.userNm = userNm;
        this.userRoleCd = userRoleCd;
        this.positionNm = positionNm;
        this.jobGradeNm = jobGradeNm;
        this.onDutyNm = onDutyNm;
        this.departmentNm = departmentNm;
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
        this.auditInfo = auditInfo;
    }
}
