package com.sk.domain.shared.entity.master.tlm;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tlm_emlsendl", indexes = {
        @Index(name = "TLM_APIACCSL_IX01", columnList = "eml_rcv_usr_logn_id")
})
@Comment("TLM_메일발송내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAccessLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eml_send_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("이메일발송일련번호")
    private Integer emailSendSequence;

    @Column(name = "eml_rcv_usr_logn_id", length = 50)
    @Comment("이메일수신사용자로그인ID")
    private String emailReceiveUserLoginId;

    @Column(name = "eml_titl_nm", nullable = false, length = 255)
    @Comment("이메일제목명")
    private String emailTitleName;

    @Lob
    @Column(name = "eml_cntt", columnDefinition = "text")
    @Comment("이메일내용")
    private String emailContent;

    @Column(name = "eml_send_stat_nm", length = 20)
    @Comment("이메일발송상태명")
    private String emailSendStatusName;

    @Lob
    @Column(name = "eml_send_err_cntt", columnDefinition = "text")
    @Comment("이메일발송오류내용")
    private String emailSendErrorContent;

    @Column(name = "eml_send_dtm", columnDefinition = "datetime")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Comment("이메일발송일시")
    private LocalDateTime emailSendDateTime;

    @Builder
    public EmailAccessLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer emailSendSequence, String emailReceiveUserLoginId,
            String emailTitleName, String emailContent, String emailSendStatusName,
            String emailSendErrorContent, LocalDateTime emailSendDateTime) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.emailSendSequence = emailSendSequence;
        this.emailReceiveUserLoginId = emailReceiveUserLoginId;
        this.emailTitleName = emailTitleName;
        this.emailContent = emailContent;
        this.emailSendStatusName = emailSendStatusName;
        this.emailSendErrorContent = emailSendErrorContent;
        this.emailSendDateTime = emailSendDateTime != null ? emailSendDateTime : LocalDateTime.now();
    }
}