package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmm.MarketIndexDataDetailId;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMM_시장지수데이터상세")
@Table(name = "tmm_mrktidxdatad")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketIndexDataDetail extends BaseEntity {

    @EmbeddedId
    private MarketIndexDataDetailId id;

    @Column(name = "instm_nm", length = 255, nullable = false)
    @Comment("INSTRUMENT명")
    private String instrumentNm;

    @Column(name = "instm_cltn_tbl_nm", length = 50, nullable = false)
    @Comment("INSTRUMENT수집테이블명")
    private String instrumentCollectionTableNm;

    @Column(name = "data_cltn_bgn_dt", length = 8)
    @Comment("데이터수집시작일자")
    private String dataCollectionBeginDt;

    @Column(name = "data_mdl_use_yn", length = 1, nullable = false, columnDefinition = "char(1)")
    @Comment("데이터모델사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isDataModelUsed;

    @Column(name = "bztgt_yn", length = 1, nullable = false, columnDefinition = "char(1)")
    @Comment("비즈타겟여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isbzTarget;

    @Column(name = "instm_ctgr_nm", length = 500)
    @Comment("INSTRUMENT카테고리명")
    private String instrumentCategoryNm;

    @Column(name = "instm_scrn_ctgr_nm", length = 500)
    @Comment("INSTRUMENT화면카테고리명")
    private String instrumentScreenCategoryNm;

    @Column(name = "instm_scrn_dtl_ctgr_nm", length = 500)
    @Comment("INSTRUMENT화면상세카테고리명")
    private String instrumentScreenDetailCategoryNm;

    @MapsId("marketIndexDataNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mrkt_idx_data_no", referencedColumnName = "mrkt_idx_data_no")
    private MarketIndexData marketIndexData;

    @Builder
    public MarketIndexDataDetail(MarketIndexDataDetailId id, String instrumentNm,
            String instrumentCollectionTableNm, String dataCollectionBeginDt, Boolean isDataModelUsed,
            Boolean isbzTarget, String instrumentCategoryNm, String instrumentScreenCategoryNm,
            String instrumentScreenDetailCategoryNm, MarketIndexData marketIndexData) {
        this.id = id;
        this.instrumentNm = instrumentNm;
        this.instrumentCollectionTableNm = instrumentCollectionTableNm;
        this.dataCollectionBeginDt = dataCollectionBeginDt;
        this.isDataModelUsed = isDataModelUsed;
        this.isbzTarget = isbzTarget;
        this.instrumentCategoryNm = instrumentCategoryNm;
        this.instrumentScreenCategoryNm = instrumentScreenCategoryNm;
        this.instrumentScreenDetailCategoryNm = instrumentScreenDetailCategoryNm;
        this.marketIndexData = marketIndexData;
    }
}
