package com.sk.domain.mi.dto.response.notification;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tlm.EmailAccessLog;
import com.sk.domain.shared.enums.EmailSendStatusName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record MiNotificationRes(
        @Schema(description = "이메일발송일련번호", example = "1") Integer emailSendSn,

        @Schema(description = "이메일제목명", example = "이메일 제목") String emailTitleNm,

        @Schema(description = "이메일내용", example = "이메일 내용") String emailCntt,

        @Schema(description = "이메일발송상태명", example = "PENDING / SENT / FAILED") EmailSendStatusName emailSendStatusNm,

        @Schema(description = "이메일발송오류내용", example = "test") String emailSendErrCntt,

        @Schema(description = "이메일발송일시", example = "2021-01-01 00:00:00") LocalDateTime emailSendDtm,

        @Schema(description = "발송성공여부", example = "true") Boolean isSendSuccess) {

    @Builder
    public MiNotificationRes(Integer emailSendSn, String emailTitleNm, String emailCntt, EmailSendStatusName emailSendStatusNm, String emailSendErrCntt,
            LocalDateTime emailSendDtm, Boolean isSendSuccess) {
        this.emailSendSn = emailSendSn;
        this.emailTitleNm = emailTitleNm;
        this.emailCntt = emailCntt;
        this.emailSendStatusNm = emailSendStatusNm;
        this.emailSendErrCntt = emailSendErrCntt;
        this.emailSendDtm = emailSendDtm;
        this.isSendSuccess = isSendSuccess;
    }

    public static MiNotificationRes of(EmailAccessLog emailAccessLog) {
        return MiNotificationRes.builder()
                .emailSendSn(emailAccessLog.getEmailSendSn())
                .emailTitleNm(emailAccessLog.getEmailTitleNm())
                .emailCntt(emailAccessLog.getEmailCntt())
                .emailSendStatusNm(emailAccessLog.getEmailSendStatusNm())
                .emailSendErrCntt(emailAccessLog.getEmailSendErrCntt())
                .emailSendDtm(emailAccessLog.getEmailSendDtm())
                .isSendSuccess(emailAccessLog.getIsSendSuccess())
                .build();
    }
}
