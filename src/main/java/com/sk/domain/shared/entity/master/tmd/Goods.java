package com.sk.domain.shared.entity.master.tmd;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_상품기본")
@Table(name = "tmd_gdsm", indexes = {
        @Index(name = "TMD_GDSM_IX01", columnList = "prvd_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goods extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("상품번호")
    private Long goodsNo;

    @Column(name = "gds_nm", nullable = false, length = 100)
    @Comment("상품명")
    private String goodsNm;

    @Column(name = "gds_cd", nullable = false, length = 20)
    @Comment("상품코드")
    private String goodsCd;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prvd_no", referencedColumnName = "prvd_no", nullable = false)
    @Comment("프로바이더번호")
    private Provider provider;
}