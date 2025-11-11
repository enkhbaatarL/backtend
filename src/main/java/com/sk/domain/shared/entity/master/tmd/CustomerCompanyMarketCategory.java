package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(
    name = "tmd_custcpnymrktctgrl",
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_custcpnymrktctgrl_ux01", columnNames = {"cust_cpny_no", "nws_mrkt_ctgr_cd"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("TMD 고객회사 시장카테고리 내역")
public class CustomerCompanyMarketCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cust_cpny_mrkt_ctgr_id", columnDefinition = "INT UNSIGNED")
    @Comment("고객회사시장카테고리ID")
    private Long customerCompanyMarketCategoryId;

    @Column(name = "cust_cpny_no", columnDefinition = "MEDIUMINT UNSIGNED", nullable = false, insertable = false, updatable = false)
    @Comment("고객회사번호")
    private Integer customerCompanyNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", foreignKey = @jakarta.persistence.ForeignKey(name = "custcpnymrktctgrl_fk01"))
    private CustomerCompany customerCompany;

    @Enumerated(EnumType.STRING)
    @Column(name = "nws_mrkt_ctgr_cd", length = 20, nullable = false)
    @Comment("뉴스시장카테고리코드 - 시장 분류 (Market Category) 원유, 천연가스, 반도체, EV배터리 등으로 분류")
    private MarketCategoryCode nwsMrktCtgrCd;

}
