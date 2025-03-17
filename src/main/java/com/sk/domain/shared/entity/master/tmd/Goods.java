package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.request.GoodsReq;
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
import lombok.Builder;
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
    private Integer goodsNo;

    @Column(name = "gds_nm", nullable = false, length = 100)
    @Comment("상품명")
    private String goodsNm;

    @Column(name = "gds_cd", nullable = false, length = 20)
    @Comment("상품코드")
    private String goodsCd;

    @Column(name = "gds_expn", length = 500)
    @Comment("상품설명")
    private String goodsExpn;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prvd_no", referencedColumnName = "prvd_no", nullable = false)
    @Comment("프로바이더번호")
    private Provider provider;

    @Builder
    public Goods(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer goodsNo, String goodsNm, String goodsCd, String goodsExpn, Boolean isUsed, Provider provider) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsCd = goodsCd;
        this.goodsExpn = goodsExpn;
        this.isUsed = isUsed;
        this.provider = provider;
    }

    public void update(GoodsReq.Update goodsReq) {

        if (StringUtils.isNotEmpty(goodsReq.goodsNm())) {
            this.goodsNm = goodsReq.goodsNm();
        }

        if (StringUtils.isNotEmpty(goodsReq.goodsCd())) {
            this.goodsCd = goodsReq.goodsCd();
        }

        if (StringUtils.isNotEmpty(goodsReq.goodsExpn())) {
            this.goodsExpn = goodsReq.goodsExpn();
        }

        if (ObjectUtils.isNotEmpty(goodsReq.isUsed())) {
            this.isUsed = goodsReq.isUsed();
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}
