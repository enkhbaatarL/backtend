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
@Comment("TMD_시나리오케이스실행거래흐름헤더내역")
@Table(
    name = "tmd_mdl_scnrcasetrdehdrl",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "tmd_mdl_scnrcasetrdehdrl_ux01",
            columnNames = {"run_seq", "selector_cd", "base_year", "axis_cd", "hdr_key"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioCaseTradeFlowHeader extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trd_hdr_seq", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("거래흐름헤더순번")
    private Long tradeHeaderSeq;

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

    @Column(name = "axis_cd", nullable = false, length = 10)
    @Comment("축코드")
    private String axisCode;

    @Column(name = "hdr_key", nullable = false, length = 220)
    @Comment("헤더키값")
    private String headerKey;

    @Column(name = "hdr_lbl_nm", nullable = false, length = 200)
    @Comment("헤더라벨명")
    private String headerLabelName;

    @Column(name = "hdr_kind_cd", nullable = false, length = 30)
    @Comment("헤더구분코드")
    private String headerKindCode;

    @Column(name = "rgn_cd", length = 50)
    @Comment("지역코드")
    private String regionCode;

    @Column(name = "nat_nm", length = 120)
    @Comment("국가명")
    private String nationName;

    @Column(name = "hdr_ord", nullable = false, columnDefinition = "int unsigned")
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Comment("헤더순서")
    private Integer headerOrder;

    public ScenarioCaseTradeFlowHeader(
        LocalDateTime createDt,
        LocalDateTime modifyDt,
        String createUserId,
        String modifyUserId,
        Boolean isDeleted,
        Long tradeHeaderSeq,
        Long runSeq,
        String selectorCode,
        Integer baseYear,
        String axisCode,
        String headerKey,
        String headerLabelName,
        String headerKindCode,
        String regionCode,
        String nationName,
        Integer headerOrder
    ) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.tradeHeaderSeq = tradeHeaderSeq;
        this.runSeq = runSeq;
        this.selectorCode = selectorCode;
        this.baseYear = baseYear;
        this.axisCode = axisCode;
        this.headerKey = headerKey;
        this.headerLabelName = headerLabelName;
        this.headerKindCode = headerKindCode;
        this.regionCode = regionCode;
        this.nationName = nationName;
        this.headerOrder = headerOrder;
    }
}
