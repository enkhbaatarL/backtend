package com.sk.domain.shared.entity.compositeKey.tmm;

import java.io.Serializable;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class MarketIndexDataDetailId implements Serializable {

    @Column(name = "mrkt_idx_data_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("시장지수데이터번호")
    private Integer marketIndexDataNo;

    @Column(name = "mrkt_idx_anly_tgt_cd", length = 15, nullable = false)
    @Comment("시장지수분석대상코드")
    private String marketIndexAnalysisTargetCd;

    @Column(name = "instm_data_spt_cd", length = 10, nullable = false)
    @Comment("INSTRUMENT데이터구분코드")
    private String instrumentDataSeparateCd;

    @Builder
    public MarketIndexDataDetailId(Integer marketIndexDataNo, String marketIndexAnalysisTargetCd,
            String instrumentDataSeparateCd) {
        this.marketIndexDataNo = marketIndexDataNo;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.instrumentDataSeparateCd = instrumentDataSeparateCd;
    }
}
