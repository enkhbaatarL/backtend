package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tmd.GoodsDataSourceDetailUnitConvertMappingId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_상품데이터단위변환내역")
@Table(name = "tmd_gdsdataunitconvl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataUnitConvert extends BaseEntity {

    @EmbeddedId
    private GoodsDataSourceDetailUnitConvertMappingId id;

    @MapsId("goodsDataSourceNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_src_no", referencedColumnName = "gds_data_src_no", nullable = false)
    @Comment("상품데이터소스상세")
    private GoodsDataSourceDetail goodsDataSourceDetail;

    @MapsId("customerCompanyNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_conv_no", referencedColumnName = "unit_conv_no", nullable = false)
    @Comment("단위변환기본")
    private UnitConvert unitConvert;

    @Builder
    public GoodsDataUnitConvert(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, GoodsDataSourceDetailUnitConvertMappingId id, GoodsDataSourceDetail goodsDataSourceDetail, UnitConvert unitConvert) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.goodsDataSourceDetail = goodsDataSourceDetail;
        this.unitConvert = unitConvert;
    }

    public static GoodsDataUnitConvert of(GoodsDataSourceDetail goodsDataSourceDetail, UnitConvert unitConvert) {
        return GoodsDataUnitConvert.builder()
                .goodsDataSourceDetail(goodsDataSourceDetail)
                .unitConvert(unitConvert)
                .build();
    }
}
