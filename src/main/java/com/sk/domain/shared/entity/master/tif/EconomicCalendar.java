package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

/**
 * 경제 지표 달력 엔티티
 */
@Entity
@Table(name = "tif_ecnmc_cal",
       uniqueConstraints = {
           @UniqueConstraint(name = "tif_ecnmc_cal_ux01", columnNames = {"code", "date"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("경제 지표 달력")
public class EconomicCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ecnmc_cal_id", columnDefinition = "bigint unsigned")
    @Comment("경제달력데이터ID")
    private Long economicCalendarId;

    @Column(name = "code", nullable = false, length = 15)
    @Comment("RIC코드")
    private String code;

    @Column(name = "date", nullable = false)
    @Comment("수신일자")
    private LocalDateTime date;

    @Column(name = "ecnmc_indcr_nm", length = 500)
    @Comment("경제지표이름")
    private String economicIndicatorName;

    @Column(name = "ecnmc_indcr_prsnttn_day", nullable = false, length = 10)
    @Comment("경제지표발표날짜")
    private String economicIndicatorPresentationDay;

    @Column(name = "ecnmc_indcr_prsnttn_tm", length = 8)
    @Comment("경제지표발표시간")
    private String economicIndicatorPresentationTime;

    @Column(name = "ecnmc_indcr_rl_val", precision = 20, scale = 4)
    @Comment("경제지표실제값")
    private BigDecimal economicIndicatorRealValue;

    @Column(name = "ecnmc_indcr_prsnttn_pr_val", precision = 20, scale = 4)
    @Comment("경제지표발표직전값")
    private BigDecimal economicIndicatorPresentationPriorValue;

    @Column(name = "ecnmc_indcr_prdc_day", length = 10)
    @Comment("경제지표예측날짜")
    private String economicIndicatorPredictionDay;

    @Column(name = "ecnmc_indcr_prdc_val", precision = 20, scale = 4)
    @Comment("경제지표예측값")
    private BigDecimal economicIndicatorPredictionValue;

    @Column(name = "unit_prefix", length = 10)
    @Comment("경제지표의 값의 단위 ( K,M 등 )")
    private String unitPrefix;

    @Column(name = "unit", length = 20)
    @Comment("경제지표 단위")
    private String unit;

    @Column(name = "nxt_ecnmc_indcr_prsnttn_day", length = 10)
    @Comment("다음경제지표발표날짜")
    private String nextEconomicIndicatorPresentationDay;

    @Builder
    public EconomicCalendar(String code, LocalDateTime date, String economicIndicatorName,
                            String economicIndicatorPresentationDay, String economicIndicatorPresentationTime,
                            BigDecimal economicIndicatorRealValue, BigDecimal economicIndicatorPresentationPriorValue,
                            String economicIndicatorPredictionDay, BigDecimal economicIndicatorPredictionValue,
                            String unitPrefix, String unit, String nextEconomicIndicatorPresentationDay) {
        this.code = code;
        this.date = date;
        this.economicIndicatorName = economicIndicatorName;
        this.economicIndicatorPresentationDay = economicIndicatorPresentationDay;
        this.economicIndicatorPresentationTime = economicIndicatorPresentationTime;
        this.economicIndicatorRealValue = economicIndicatorRealValue;
        this.economicIndicatorPresentationPriorValue = economicIndicatorPresentationPriorValue;
        this.economicIndicatorPredictionDay = economicIndicatorPredictionDay;
        this.economicIndicatorPredictionValue = economicIndicatorPredictionValue;
        this.unitPrefix = unitPrefix;
        this.unit = unit;
        this.nextEconomicIndicatorPresentationDay = nextEconomicIndicatorPresentationDay;
    }

}
