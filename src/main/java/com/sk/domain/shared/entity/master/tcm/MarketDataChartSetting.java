package com.sk.domain.shared.entity.master.tcm;

import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

@Getter
@Entity
@Table(name = "tcm_mrktdatachrtsetl")
@Comment("TCM_시장데이터차트설정내역")
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketDataChartSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mrkt_data_set_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("마켓데이터설정번호")
    private Integer marketDataSettingNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no", nullable = false)
    @Comment("고객회사번호")
    private CustomerCompany customerCompany;

    @Column(name = "chrt_mark_rnk", nullable = false)
    @Comment("차트표시순위")
    private Short chartMarkRank;

    @Column(name = "cmprs_cd1", nullable = false)
    @Comment("수치비교코드1")
    private String compareCode1;
    @Column(name = "cmprs_cd2", nullable = false)
    @Comment("수치비교코드2")
    private String compareCode2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", referencedColumnName = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스번호")
    private GoodsDataSourceDetail goodsDataSourceDetail;

    @Transient
    public Set<CalculatePeriodChangeType> toCalculatePeriodChangeTypes(){
        // 순서 중요
        HashSet<CalculatePeriodChangeType> result = new LinkedHashSet<>(2);
        if(compareCode1 != null && !compareCode1.isBlank()){
            try{
                result.add(CalculatePeriodChangeType.valueOf(compareCode1));
            }catch (IllegalArgumentException ignored){
                log.debug("{} is not CalculatePeriodChangeType. MarketDataSettingNo={}", compareCode1, marketDataSettingNo);
            }
        }
        if(compareCode2 != null && !compareCode2.isBlank()){
            try{
                result.add(CalculatePeriodChangeType.valueOf(compareCode2));
            }catch (IllegalArgumentException ignored){
                log.debug("{} is not CalculatePeriodChangeType. MarketDataSettingNo={}", compareCode2, marketDataSettingNo);
            }
        }
        if(result.size() < 2){
            result.add(CalculatePeriodChangeType.DOD);
        }
        if(result.size() < 2){
            result.add(CalculatePeriodChangeType.WOW);
        }
        return result;
    }

}
