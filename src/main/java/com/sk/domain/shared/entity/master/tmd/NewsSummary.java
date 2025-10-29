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
        @Index(name = "tmd_nws_smry_ix_01", columnList = "data_reg_sn, first_created"),
        @Index(name = "tmd_nws_smry_ix_02", columnList = "data_reg_sn, first_created, crtr_dt")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nws_smry_id", nullable = false, columnDefinition = "int unsigned")
    @Comment("뉴스 요약 ID")
    private Long newsSummaryId;

    @Column(name = "data_reg_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("Refinitiv 뉴스 데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "first_created", nullable = false)
    @Comment("Refinitiv 뉴스 최초생성일시")
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

    @Column(name = "brf_smry", length = 200)
    @Comment("간략요약")
    private String briefSummary;

    @Column(name = "smry", length = 500)
    @Comment("요약")
    private String summary;

    @Column(name = "provider", length = 50)
    @Comment("프로바이더")
    private String provider;

    @Column(name = "source", length = 100)
    @Comment("소스")
    private String source;

    @Column(name = "org_link", columnDefinition = "text")
    @Comment("원문링크")
    private String originalLink;

    @Column(name = "kwd", columnDefinition = "text")
    @Comment("핵심단어")
    private String keyword;

    @Builder
    public NewsSummary(LocalDateTime createDt, LocalDateTime modifyDt,
        String createUserId, String modifyUserId, Boolean isDeleted,
        Long newsSummaryId, Integer dataRegistrationSn, LocalDateTime firstCreated,
        RefinitivNewsData refinitivNewsData, String briefSummary, String summary,
        String provider, String source, String originalLink, String keyword) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.newsSummaryId = newsSummaryId;
        this.dataRegistrationSn = dataRegistrationSn;
        this.firstCreated = firstCreated;
        this.refinitivNewsData = refinitivNewsData;
        this.briefSummary = briefSummary;
        this.summary = summary;
        this.provider = provider;
        this.source = source;
        this.originalLink = originalLink;
        this.keyword = keyword;
    }
}
