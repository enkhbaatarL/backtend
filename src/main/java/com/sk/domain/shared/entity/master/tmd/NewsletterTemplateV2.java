package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터템플릿V2기본")
@Table(name = "tmd_nwsltrtmpltm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplateV2 extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_tmplt_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿V2ID")
    private Long newsletterTemplateV2Id;

    @Column(name = "nws_ltr_mstr_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스레터마스터ID")
    private Integer newsletterMasterId;

    @Column(name = "tmplt_nm", nullable = false, length = 200)
    @Comment("템플릿명")
    private String templateName;

    @Column(name = "tmplt_desc", length = 500)
    @Comment("템플릿설명")
    private String templateDescription;

    @Column(name = "schm_ver", nullable = false, columnDefinition = "smallint unsigned")
    @Comment("스키마버전")
    private Integer schemaVersion;

    @Column(name = "tmplt_ver_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("템플릿버전번호")
    private Integer templateVersionNo;

    @Builder
    public NewsletterTemplateV2(
        Long newsletterTemplateV2Id,
        Integer newsletterMasterId,
        String templateName,
        String templateDescription,
        Integer schemaVersion,
        Integer templateVersionNo
    ) {
        this.newsletterTemplateV2Id = newsletterTemplateV2Id;
        this.newsletterMasterId = newsletterMasterId;
        this.templateName = templateName;
        this.templateDescription = templateDescription;
        this.schemaVersion = schemaVersion;
        this.templateVersionNo = templateVersionNo;
    }
}
