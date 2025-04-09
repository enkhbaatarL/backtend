package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.listener.GoodsListener;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tmd_gdsm")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(GoodsListener.class)
public class Goods extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("상품번호")
    private Integer goodsNo;

    @Column(name = "gds_nm", nullable = false, length = 100)
    @Comment("상품명")
    private String goodsNm;

    @Column(name = "gds_id", nullable = false, length = 20)
    @Comment("상품ID")
    private String goodsId;

    @Column(name = "gds_expn", length = 500)
    @Comment("상품설명")
    private String goodsExpn;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "cpny_comm_use_psbl_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("회사공통사용가능여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isPublic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prvd_no", referencedColumnName = "prvd_no", nullable = false)
    @Comment("프로바이더번호")
    private Provider provider;

    @Builder
    public Goods(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer goodsNo, String goodsNm, String goodsId, String goodsExpn, Boolean isUsed, Boolean isPublic,
            Provider provider) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsId = goodsId;
        this.goodsExpn = goodsExpn;
        this.isUsed = isUsed;
        this.isPublic = isPublic;
        this.provider = provider;
    }

    public static Goods of(String goodsNm, String goodsId, String goodsExpn, Boolean isUsed, Boolean isPublic, Provider provider) {
        return Goods.builder()
                .goodsNm(goodsNm)
                .goodsId(goodsId)
                .goodsExpn(goodsExpn)
                .isUsed(isUsed)
                .isPublic(isPublic)
                .provider(provider)
                .build();
    }

    public void update(String goodsNm, String goodsId, String goodsExpn, Boolean isUsed, Boolean isPublic) {

        if (StringUtils.isNotEmpty(goodsNm)) {
            this.goodsNm = goodsNm;
        }

        if (StringUtils.isNotEmpty(goodsId)) {
            this.goodsId = goodsId;
        }

        if (StringUtils.isNotEmpty(goodsExpn)) {
            this.goodsExpn = goodsExpn;
        }

        if (ObjectUtils.isNotEmpty(isUsed)) {
            this.isUsed = isUsed;
        }

        if (ObjectUtils.isNotEmpty(isPublic)) {
            this.isPublic = isPublic;
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}
