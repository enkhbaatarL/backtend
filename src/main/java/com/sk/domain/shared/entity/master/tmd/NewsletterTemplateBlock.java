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
@Comment("TMD_뉴스레터템플릿블록")
@Table(name = "tmd_nwsltrtmpltblk")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsletterTemplateBlock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_ltr_tmplt_blk_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿블록ID")
    private Long newsletterTemplateBlockId;

    @Column(name = "nws_ltr_tmplt_sctn_id", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("뉴스레터템플릿섹션ID")
    private Long sectionId;

    @Column(name = "blk_cd", nullable = false, length = 50)
    @Comment("블록코드")
    private String blockCode;

    @Column(name = "blk_type_cd", nullable = false, length = 30)
    @Comment("블록타입코드")
    private String blockTypeCode;

    @Column(name = "blk_fmt_cd", nullable = false, length = 30)
    @Comment("블록형식코드")
    private String blockFormatCode;

    @Column(name = "blk_nm", nullable = false, length = 200)
    @Comment("블록명")
    private String blockName;

    @Column(name = "blk_ord", nullable = false, columnDefinition = "int unsigned")
    @Comment("블록순서")
    private Integer blockOrder;

    @Column(name = "vsblty_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("노출여부")
    private String visibilityYn;

    @Column(name = "plot_on_chrt_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("차트변곡점표시여부")
    private String plotOnChartYn;

    @Column(name = "tgt_cmpny_no", columnDefinition = "int unsigned")
    @Comment("대상고객사번호")
    private Integer targetCompanyNo;

    @Column(name = "data_src_type_cd", length = 30)
    @Comment("데이터소스타입")
    private String dataSourceTypeCode;

    @Column(name = "data_src_ref", length = 500)
    @Comment("데이터소스참조")
    private String dataSourceRef;

    @Builder
    public NewsletterTemplateBlock(
        Long newsletterTemplateBlockId,
        Long sectionId,
        String blockCode,
        String blockTypeCode,
        String blockFormatCode,
        String blockName,
        Integer blockOrder,
        String visibilityYn,
        String plotOnChartYn,
        Integer targetCompanyNo,
        String dataSourceTypeCode,
        String dataSourceRef
    ) {
        this.newsletterTemplateBlockId = newsletterTemplateBlockId;
        this.sectionId = sectionId;
        this.blockCode = blockCode;
        this.blockTypeCode = blockTypeCode;
        this.blockFormatCode = blockFormatCode;
        this.blockName = blockName;
        this.blockOrder = blockOrder;
        this.visibilityYn = visibilityYn;
        this.plotOnChartYn = plotOnChartYn;
        this.targetCompanyNo = targetCompanyNo;
        this.dataSourceTypeCode = dataSourceTypeCode;
        this.dataSourceRef = dataSourceRef;
    }
}
