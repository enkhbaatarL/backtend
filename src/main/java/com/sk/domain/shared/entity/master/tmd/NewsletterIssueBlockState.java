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
@Comment("TMD_뉴스레터원본이슈블록상태")
@Table(
    name = "tmd_nwsltrorgissblkstat",
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nwsltrorgissblkstat_ux01", columnNames = {"nws_ltr_org_iss_id", "nws_ltr_tmplt_blk_id"}),
        @UniqueConstraint(name = "tmd_nwsltrorgissblkstat_ux02", columnNames = {"nws_ltr_org_iss_sctn_stat_id", "blk_ord"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterIssueBlockState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_org_iss_blk_stat_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈블록상태ID")
    private Long newsletterIssueBlockStateId;

    @Column(name = "nws_ltr_org_iss_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈ID")
    private Long issueOriginalId;

    @Column(name = "nws_ltr_org_iss_sctn_stat_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터원본이슈섹션상태ID")
    private Long sectionStateId;

    @Column(name = "nws_ltr_tmplt_blk_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿블록ID")
    private Long templateBlockId;

    @Column(name = "blk_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("블록순서")
    private Integer blockOrder;

    @Column(name = "vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("노출여부")
    private String visibilityYn;

    @Column(name = "blk_titl", length = 200)
    @Comment("블록제목오버라이드")
    private String blockTitle;

    @Column(name = "tgt_cmpny_no", columnDefinition = "int unsigned")
    @Comment("대상고객사번호")
    private Integer targetCompanyNo;

    @Column(name = "blk_stat_json", columnDefinition = "json")
    @Comment("블록상태JSON")
    private String blockStateJson;

    @Builder
    public NewsletterIssueBlockState(
        Long newsletterIssueBlockStateId,
        Long issueOriginalId,
        Long sectionStateId,
        Long templateBlockId,
        Integer blockOrder,
        String visibilityYn,
        String blockTitle,
        Integer targetCompanyNo,
        String blockStateJson
    ) {
        this.newsletterIssueBlockStateId = newsletterIssueBlockStateId;
        this.issueOriginalId = issueOriginalId;
        this.sectionStateId = sectionStateId;
        this.templateBlockId = templateBlockId;
        this.blockOrder = blockOrder;
        this.visibilityYn = visibilityYn;
        this.blockTitle = blockTitle;
        this.targetCompanyNo = targetCompanyNo;
        this.blockStateJson = blockStateJson;
    }
}
