package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스레터원본이슈섹션상태")
@Table(
    name = "tmd_nwsltrorgisssctnstat",
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nwsltrorgisssctnstat_ux01", columnNames = {"nws_ltr_org_iss_id", "nws_ltr_tmplt_sctn_id"}),
        @UniqueConstraint(name = "tmd_nwsltrorgisssctnstat_ux02", columnNames = {"nws_ltr_org_iss_id", "sctn_ord"}),
        @UniqueConstraint(name = "tmd_nwsltrorgisssctnstat_ux03", columnNames = {"nws_ltr_org_iss_sctn_stat_id", "nws_ltr_org_iss_id"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterIssueSectionState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_org_iss_sctn_stat_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈섹션상태ID")
    private Long newsletterIssueSectionStateId;

    @Column(name = "nws_ltr_org_iss_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈ID")
    private Long issueOriginalId;

    @Column(name = "nws_ltr_tmplt_sctn_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿섹션ID")
    private Long templateSectionId;

    @Column(name = "sctn_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("섹션순서")
    private Integer sectionOrder;

    @Column(name = "vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("노출여부")
    private String visibilityYn;

    @Column(name = "sctn_titl", length = 200)
    @Comment("섹션제목오버라이드")
    private String sectionTitle;

    @Column(name = "tgt_cmpny_no", columnDefinition = "int unsigned")
    @Comment("대상고객사번호")
    private Integer targetCompanyNo;

    @Column(name = "sctn_stat_json", columnDefinition = "json")
    @Comment("섹션상태JSON")
    private String sectionStateJson;

    @Builder
    public NewsletterIssueSectionState(
        Long newsletterIssueSectionStateId,
        Long issueOriginalId,
        Long templateSectionId,
        Integer sectionOrder,
        String visibilityYn,
        String sectionTitle,
        Integer targetCompanyNo,
        String sectionStateJson
    ) {
        this.newsletterIssueSectionStateId = newsletterIssueSectionStateId;
        this.issueOriginalId = issueOriginalId;
        this.templateSectionId = templateSectionId;
        this.sectionOrder = sectionOrder;
        this.visibilityYn = visibilityYn;
        this.sectionTitle = sectionTitle;
        this.targetCompanyNo = targetCompanyNo;
        this.sectionStateJson = sectionStateJson;
    }
}
