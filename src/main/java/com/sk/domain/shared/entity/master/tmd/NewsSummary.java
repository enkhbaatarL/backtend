package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tif.RefinitivNewsData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TMD_뉴스요약")
@Table(name = "tmd_nws_smry",
    indexes = {
        @Index(name = "tmd_nws_smry_ix01", columnList = "prvd_id, data_reg_sn, first_created"),
        @Index(name = "tmd_nws_smry_ix02", columnList = "prvd_id, data_reg_sn, first_created, crtr_dt"),
        @Index(name = "tmd_nws_smry_ix03", columnList = "crtr_dt, simlr_grp_stat_cd"),
        @Index(name = "tmd_nws_smry_ix04", columnList = "gds_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_nws_smry_ux01", columnNames = {"gds_id", "data_reg_sn", "first_created"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_smry_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스 요약 ID")
    private Long newsSummaryId;

    @Column(name = "data_reg_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "prvd_id", nullable = false, length = 20)
    @Comment("프로바이더ID")
    private String providerId;

    @Column(name = "gds_id", nullable = false, length = 20)
    @Comment("상품ID")
    private String goodsId;

    @Column(name = "first_created", nullable = false)
    @Comment("NEWS최초생성일시")
    private LocalDateTime firstCreated;

    // 논리적 연관관계 (조회용, insertable=false, updatable=false)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_reg_sn", referencedColumnName = "data_reg_sn",
        insertable = false, updatable = false)
    @JoinColumn(name = "first_created", referencedColumnName = "first_created",
        insertable = false, updatable = false)
    @Comment("Refinitiv 뉴스 데이터")
    private RefinitivNewsData refinitivNewsData;

    @Column(name = "crtr_dt", length = 8, nullable = false)
    @Comment("기준 일자")
    private String crtrDt;

    @Column(name = "brf_smry", length = 500)
    @Comment("간략요약")
    private String briefSummary;

    @Column(name = "smry", length = 1000)
    @Comment("요약")
    private String summary;

    @Column(name = "brf_smry_en", length = 500)
    @Comment("간략 요약 영문")
    private String briefSummaryEn;

    @Column(name = "smry_en", length = 1000)
    @Comment("요약 영문")
    private String summaryEn;

    @Column(name = "simlr_grp_stat_cd", length = 10, nullable = false, columnDefinition = "varchar(10) default 'NONE'")
    @Comment("유사 그룹 상태 코드 (INCL:포함, NONE:비포함, HEAD:대표뉴스)")
    private String similarGroupStatusCode = "NONE";

    @Builder
    public NewsSummary(LocalDateTime createDt, LocalDateTime modifyDt,
        String createUserId, String modifyUserId, Boolean isDeleted,
        Long newsSummaryId, Integer dataRegistrationSn, String providerId, String goodsId, LocalDateTime firstCreated,
        RefinitivNewsData refinitivNewsData, String crtrDt, String briefSummary, String summary,
        String briefSummaryEn, String summaryEn, String similarGroupStatusCode) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsSummaryId = newsSummaryId;
        this.dataRegistrationSn = dataRegistrationSn;
        this.providerId = providerId;
        this.goodsId = goodsId;
        this.firstCreated = firstCreated;
        this.refinitivNewsData = refinitivNewsData;
        this.crtrDt = crtrDt;
        this.briefSummary = briefSummary;
        this.summary = summary;
        this.briefSummaryEn = briefSummaryEn;
        this.summaryEn = summaryEn;
        this.similarGroupStatusCode = similarGroupStatusCode != null ? similarGroupStatusCode : "NONE";
    }

}
