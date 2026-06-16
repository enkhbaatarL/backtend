package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.converter.StringToPeriodUnitSeparateCdConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 시나리오 케이스 실행 가격 결과 (run_seq 단위 누적 적재, Agent 측 INSERT — backend는 조회 전용).
 */
@Getter
@Entity
@Comment("TMD_시나리오케이스실행가격내역")
@Table(
    name = "tmd_mdl_scnrcaseprcl",
    uniqueConstraints = {
        @UniqueConstraint(name = "tmd_mdl_scnrcaseprcl_ux01", columnNames = {"run_seq", "prc_rgn_nm", "perd_key"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCaseRunPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prc_seq", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("가격순번")
    private Long priceSeq;

    @Column(name = "run_seq", nullable = false, columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("실행순번 (tmd_mdl_scnrcaseh.run_seq FK)")
    private Long runSeq;

    @Convert(converter = StringToPeriodUnitSeparateCdConverter.class)
    @Column(name = "perd_unit_spt_cd", nullable = false, length = 2)
    @Comment("기간단위구분코드 (M:월 / Y:년)")
    private PeriodUnitSeparateCd periodUnit;

    @Column(name = "perd_key", nullable = false, length = 6)
    @Comment("기간키값 (YYYY 또는 YYYYMM)")
    private String periodKey;

    @Column(name = "prc_rgn_nm", nullable = false, length = 200)
    @Comment("가격지역명 (JKM / TTF / HH)")
    private String priceRegionName;

    @Column(name = "base_case_prc", nullable = false)
    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Comment("베이스케이스가격 (단위: USD/MMBtu 고정)")
    private BigDecimal baseCasePrice;

    @Column(name = "user_adj_prc", nullable = false)
    @JdbcTypeCode(SqlTypes.DOUBLE)
    @Comment("사용자조정가격 (단위: USD/MMBtu 고정)")
    private BigDecimal userAdjustedPrice;

}
