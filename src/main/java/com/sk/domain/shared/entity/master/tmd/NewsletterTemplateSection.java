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
@Comment("TMD_뉴스레터템플릿섹션")
@Table(name = "tmd_nwsltrtmpltsctn")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplateSection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_tmplt_sctn_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿섹션ID")
    private Long newsletterTemplateSectionId;

    @Column(name = "nws_ltr_tmplt_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿V2ID")
    private Long templateV2Id;

    @Column(name = "sctn_cd", nullable = false, length = 50)
    @Comment("섹션코드")
    private String sectionCode;

    @Column(name = "sctn_type_cd", nullable = false, length = 30)
    @Comment("섹션유형코드")
    private String sectionTypeCode;

    @Column(name = "sctn_nm", nullable = false, length = 200)
    @Comment("섹션명")
    private String sectionName;

    @Column(name = "sctn_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("섹션순서")
    private Integer sectionOrder;

    @Column(name = "vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("노출여부")
    private String visibilityYn;

    @Column(name = "tgt_cmpny_no", columnDefinition = "int unsigned")
    @Comment("대상고객사번호")
    private Integer targetCompanyNo;

    @Builder
    public NewsletterTemplateSection(
        Long newsletterTemplateSectionId,
        Long templateV2Id,
        String sectionCode,
        String sectionTypeCode,
        String sectionName,
        Integer sectionOrder,
        String visibilityYn,
        Integer targetCompanyNo
    ) {
        this.newsletterTemplateSectionId = newsletterTemplateSectionId;
        this.templateV2Id = templateV2Id;
        this.sectionCode = sectionCode;
        this.sectionTypeCode = sectionTypeCode;
        this.sectionName = sectionName;
        this.sectionOrder = sectionOrder;
        this.visibilityYn = visibilityYn;
        this.targetCompanyNo = targetCompanyNo;
    }
}
