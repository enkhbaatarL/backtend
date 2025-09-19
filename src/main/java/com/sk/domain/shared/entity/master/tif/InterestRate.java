package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Comment("TIF_Interest Rates_수신")
@Table(name = "tif_interestratesr")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestRate extends BaseEntity {

    /** 미국 10년물 국채금리 */
    public static final String GOV10Y = "TRUS10T";
    /** 미국 2년물 국채금리 */
    public static final String GOV2Y = "TRUS2YT";
    /** 미국 10년물-2년물 국채금리 스프래드 */
    public static final String GOV10Y2Y_SPREAD = "TRUS10T-TRUS2YT";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_reg_sn", nullable = false)
    @Comment("데이터등록일련번호")
    private Integer dataRegistrationSn;

    @Column(name = "code", nullable = false, length = 30)
    @Comment("원천ID코드")
    private String code;

    @Column(name = "date", nullable = false)
    @Comment("원천전달데이터일시")
    private LocalDateTime date;

    @Column(name = "ry", precision = 15, scale = 4)
    @Comment("만기수익률")
    @Setter
    private BigDecimal ry;
    @Column(name = "pb", precision = 15, scale = 4)
    @Comment("입찰가격")
    @Setter
    private BigDecimal pb;


    @Builder
    public InterestRate(Integer dataRegistrationSn, String code, LocalDateTime date, BigDecimal ry,
        BigDecimal pb) {
        this.dataRegistrationSn = dataRegistrationSn;
        this.code = code;
        this.date = date;
        this.ry = ry;
        this.pb = pb;
    }

    public static InterestRate of(String code, LocalDateTime date, BigDecimal ry, BigDecimal pb) {
        return InterestRate.builder()
            .code(code)
            .date(date)
            .ry(ry)
            .pb(pb)
            .build();
    }
}
