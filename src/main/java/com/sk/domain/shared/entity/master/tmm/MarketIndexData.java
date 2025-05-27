package com.sk.domain.shared.entity.master.tmm;

import org.hibernate.annotations.Comment;

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

@Getter
@Entity
@Comment("TMM_시장지수데이터기본")
@Table(name = "tmm_mrktidxdatam")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketIndexData extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_idx_data_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("시장지수데이터번호")
    private Integer marketIndexDataNo;

    @Column(name = "ric_cd", length = 15)
    @Comment("RIC코드")
    private String ricCd;

    @Column(name = "isin_cd", length = 15, nullable = false)
    @Comment("ISIN코드")
    private String isinCd;

    @Column(name = "mnmnc_cd", length = 50, nullable = false)
    @Comment("MNEMONIC코드")
    private String mnemonicCd;

    @Column(name = "nat_cd", length = 5, nullable = false)
    @Comment("국가코드")
    private String nationCd;

    @Column(name = "crcy_cd", length = 5, nullable = false)
    @Comment("통화코드")
    private String currencyCd;

    @Builder
    public MarketIndexData(String ricCd, String isinCd, String mnemonicCd, String nationCd, String currencyCd) {
        this.ricCd = ricCd;
        this.isinCd = isinCd;
        this.mnemonicCd = mnemonicCd;
        this.nationCd = nationCd;
        this.currencyCd = currencyCd;
    }
}
