package com.sk.domain.shared.entity.master.tlm;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.GoodsCriticalValueSetting;
import com.sk.domain.shared.enums.EmailSendStatusName;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tlm_emlsendl")
@Comment("TLM_메일발송내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAccessLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eml_send_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("이메일발송일련번호")
    private Integer emailSendSn;

    @Column(name = "eml_rcv_usr_logn_id", length = 50)
    @Comment("이메일수신사용자로그인ID")
    private String emailReceiveUserLoginId;

    @Column(name = "eml_titl_nm", nullable = false, length = 255)
    @Comment("이메일제목명")
    private String emailTitleNm;

    @Lob
    @Column(name = "eml_cntt", columnDefinition = "text")
    @Comment("이메일내용")
    private String emailCntt;

    @Enumerated(EnumType.STRING)
    @Column(name = "eml_send_stat_nm", length = 20)
    @Comment("이메일발송상태명")
    private EmailSendStatusName emailSendStatusNm;

    @Lob
    @Column(name = "eml_send_err_cntt", columnDefinition = "text")
    @Comment("이메일발송오류내용")
    private String emailSendErrCntt;

    @Column(name = "eml_send_dtm", columnDefinition = "datetime")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Comment("이메일발송일시")
    private LocalDateTime emailSendDtm;

    @Column(name = "send_succ_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("발송성공여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isSendSuccess;

    @Column(name = "eml_inqr_yn", nullable = false, columnDefinition = "char(1)")
    @Comment("이메일조회여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isEmailInquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_gds_noti_set_no", referencedColumnName = "usr_gds_noti_set_no", nullable = true)
    @Comment("사용자상품알림설정번호")
    private GoodsCriticalValueSetting goodsCriticalValueSetting;

    @Builder
    public EmailAccessLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer emailSendSn, String emailReceiveUserLoginId,
            String emailTitleNm, String emailCntt, EmailSendStatusName emailSendStatusNm,
            String emailSendErrCntt, LocalDateTime emailSendDtm, Boolean isSendSuccess,
            Boolean isEmailInquiry, GoodsCriticalValueSetting goodsCriticalValueSetting) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.emailSendSn = emailSendSn;
        this.emailReceiveUserLoginId = emailReceiveUserLoginId;
        this.emailTitleNm = emailTitleNm;
        this.emailCntt = emailCntt;
        this.emailSendStatusNm = emailSendStatusNm;
        this.emailSendErrCntt = emailSendErrCntt;
        this.emailSendDtm = emailSendDtm != null ? emailSendDtm : LocalDateTime.now();
        this.isSendSuccess = isSendSuccess;
        this.isEmailInquiry = isEmailInquiry;
        this.goodsCriticalValueSetting = goodsCriticalValueSetting;
    }

    public static EmailAccessLog of(String emailReceiveUserLoginId, String emailTitleNm, String emailCntt,
            EmailSendStatusName emailSendStatusNm, GoodsCriticalValueSetting goodsCriticalValueSetting) {
        return EmailAccessLog.builder()
                .emailReceiveUserLoginId(emailReceiveUserLoginId)
                .emailTitleNm(emailTitleNm)
                .emailCntt(emailCntt)
                .emailSendStatusNm(emailSendStatusNm)
                .emailSendDtm(LocalDateTime.now())
                .isSendSuccess(false)
                .isEmailInquiry(false)
                .goodsCriticalValueSetting(goodsCriticalValueSetting)
                .build();
    }

    public void sendEmailSuccessUpdate(EmailSendStatusName emailSendStatusNm) {
        this.emailSendStatusNm = emailSendStatusNm;
        this.isSendSuccess = true;
    }

    public void sendEmailFailUpdate(EmailSendStatusName emailSendStatusNm, String emailSendErrCntt) {
        this.emailSendStatusNm = emailSendStatusNm;
        this.emailSendErrCntt = emailSendErrCntt;
        this.isSendSuccess = false;
    }

    public void updateInquiry() {
        this.isEmailInquiry = true;
    }
}