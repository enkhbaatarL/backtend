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
@Comment("TMD_시나리오케이스실행거래흐름내역")
@Table(
    name = "tmd_mdl_scnrcasetrdeflwl",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "tmd_mdl_scnrcasetrdeflwl_ux01",
            columnNames = {"run_seq", "selector_cd", "base_year", "from_hdr_key", "to_hdr_key"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCaseTradeFlow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trd_flw_seq", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("거래흐름순번")
    private Long tradeFlowSeq;

    @Column(name = "run_seq", nullable = false, columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("실행순번")
    private Long runSeq;

    @Column(name = "selector_cd", nullable = false, length = 10)
    @Comment("선택코드")
    private String selectorCode;

    @Column(name = "base_year", nullable = false, columnDefinition = "smallint unsigned")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Comment("기준연도")
    private Integer baseYear;

    @Column(name = "from_hdr_key", nullable = false, length = 220)
    @Comment("From 헤더키값")
    private String fromHeaderKey;

    @Column(name = "to_hdr_key", nullable = false, length = 220)
    @Comment("To 헤더키값")
    private String toHeaderKey;

    @Column(name = "from_rgn_cd", length = 50)
    @Comment("From 지역코드")
    private String fromRegionCode;

    @Column(name = "from_nat_nm", length = 120)
    @Comment("From 국가명")
    private String fromNationName;

    @Column(name = "to_rgn_cd", length = 50)
    @Comment("To 지역코드")
    private String toRegionCode;

    @Column(name = "to_nat_nm", length = 120)
    @Comment("To 국가명")
    private String toNationName;

    @Column(name = "flow_val", nullable = false)
    @Comment("흐름값")
    private Double flowValue;

    public ScenarioCaseTradeFlow(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        Long tradeFlowSeq,
        Long runSeq,
        String selectorCode,
        Integer baseYear,
        String fromHeaderKey,
        String toHeaderKey,
        String fromRegionCode,
        String fromNationName,
        String toRegionCode,
        String toNationName,
        Double flowValue
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.tradeFlowSeq = tradeFlowSeq;
        this.runSeq = runSeq;
        this.selectorCode = selectorCode;
        this.baseYear = baseYear;
        this.fromHeaderKey = fromHeaderKey;
        this.toHeaderKey = toHeaderKey;
        this.fromRegionCode = fromRegionCode;
        this.fromNationName = fromNationName;
        this.toRegionCode = toRegionCode;
        this.toNationName = toNationName;
        this.flowValue = flowValue;
    }
}
