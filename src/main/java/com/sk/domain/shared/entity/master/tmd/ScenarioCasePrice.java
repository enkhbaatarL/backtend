package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
public class ScenarioCasePrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prc_seq", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("가격순번")
    private Long priceSeq;

    @Column(name = "run_seq", nullable = false, columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("실행순번")
    private Long runSeq;

    @Column(name = "perd_unit_spt_cd", nullable = false, length = 2)
    @Comment("기간단위구분코드")
    private String periodUnitSeparateCode;

    @Column(name = "perd_key", nullable = false, length = 6)
    @Comment("기간키값")
    private String periodKey;

    @Column(name = "prc_rgn_nm", nullable = false, length = 200)
    @Comment("가격지역명")
    private String priceRegionName;

    @Column(name = "base_case_prc", nullable = false)
    @Comment("베이스케이스가격")
    private Double baseCasePrice;

    @Column(name = "user_adj_prc", nullable = false)
    @Comment("사용자조정가격")
    private Double userAdjustedPrice;

    public ScenarioCasePrice(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        Long priceSeq,
        Long runSeq,
        String periodUnitSeparateCode,
        String periodKey,
        String priceRegionName,
        Double baseCasePrice,
        Double userAdjustedPrice
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.priceSeq = priceSeq;
        this.runSeq = runSeq;
        this.periodUnitSeparateCode = periodUnitSeparateCode;
        this.periodKey = periodKey;
        this.priceRegionName = priceRegionName;
        this.baseCasePrice = baseCasePrice;
        this.userAdjustedPrice = userAdjustedPrice;
    }
}
