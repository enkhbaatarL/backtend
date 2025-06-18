package com.sk.domain.shared.entity.master.tcm;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.dto.request.rpa.ComponentInfoReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.enums.ComponentType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tcm_cmpntinfom")
@Comment("TCM_컴포넌트정보기본")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComponentInfo extends BaseEntity {

    @Id
    @Column(name = "cmpnt_type_id", nullable = false, length = 100)
    @Comment("컴포넌트유형ID")
    private String componentTypeId;

    @Column(name = "cmpnt_type_nm", nullable = false, length = 100)
    @Comment("컴포넌트유형명")
    private String componentTypeNm;

    @Column(name = "gds_data_src_nm", nullable = false, length = 100)
    @Comment("상품데이터소스명")
    private String goodsDataSourceNm;

    @Column(name = "tgt_db_nm", nullable = false, length = 100)
    @Comment("대상데이터베이스명")
    private String targetDbNm;

    @Column(name = "tgt_tbl_nm", nullable = false, length = 50)
    @Comment("대상테이블명")
    private String targetTableNm;

    @Column(name = "qry_cdtn_cntt", length = 1000)
    @Comment("쿼리조건내용")
    private String queryConditionCntt;

    @Column(name = "tbl_colm_list_nm", length = 500)
    @Comment("테이블컬럼목록명")
    private String tableColumnListNm;

    @Column(name = "exl_colm_list_nm", length = 500)
    @Comment("엑셀컬럼목록명")
    private String excelColumnListNm;

    @Column(name = "exl_tbl_colm_list_nm", length = 500)
    @Comment("엑셀테이블컬럼목록명")
    private String excelTableColumnListNm;

    @Column(name = "exl_cdtn_cntt", length = 1000)
    @Comment("엑셀조건내용")
    private String excelConditionCntt;

    @Column(name = "gds_data_src_unit_nm", length = 50)
    @Comment("상품데이터소스단위명")
    private String goodsDataSourceUnitNm;

    @Column(name = "gds_data_src_crcy_unit_nm", length = 50)
    @Comment("상품데이터소스통화단위명")
    private String goodsDataSourceCurrencyUnitNm;

    @Column(name = "cndlc_psbl_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @ColumnDefault("'N'")
    @Comment("캔들차트가능여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isCandleChartPossible;

    @Column(name = "conv_tgt_dtl_type_nm", length = 50)
    @Comment("변환대상상세유형명")
    private String convertTargetDetailTypeNm;  

    @Enumerated(EnumType.STRING)
    @Column(name = "data_strt_conv_nm", length = 50)
    @Comment("데이터구조변환명")
    private ComponentType dataStructureConvertNm;

    @Builder
    public ComponentInfo(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, String componentTypeId, String componentTypeNm, String goodsDataSourceNm,
            String targetDbNm, String targetTableNm, String queryConditionCntt,
            String tableColumnListNm, String excelColumnListNm, String excelTableColumnListNm, String excelConditionCntt, String goodsDataSourceUnitNm,
            String goodsDataSourceCurrencyUnitNm, Boolean isCandleChartPossible, String convertTargetDetailTypeNm,
            ComponentType dataStructureConvertNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.componentTypeId = componentTypeId;
        this.componentTypeNm = componentTypeNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionCntt = queryConditionCntt;
        this.tableColumnListNm = tableColumnListNm;
        this.excelColumnListNm = excelColumnListNm;
        this.excelTableColumnListNm = excelTableColumnListNm;
        this.excelConditionCntt = excelConditionCntt;
        this.goodsDataSourceUnitNm = goodsDataSourceUnitNm;
        this.goodsDataSourceCurrencyUnitNm = goodsDataSourceCurrencyUnitNm;
        this.isCandleChartPossible = isCandleChartPossible;
        this.convertTargetDetailTypeNm = convertTargetDetailTypeNm;
        this.dataStructureConvertNm = dataStructureConvertNm;
    }

    public static ComponentInfo of(ComponentInfoReq.Insert dto) {
        return ComponentInfo.builder()
                .componentTypeId(dto.componentTypeId())
                .componentTypeNm(dto.componentTypeNm())
                .goodsDataSourceNm(dto.goodsDataSourceNm())
                .targetDbNm(dto.targetDbNm())
                .targetTableNm(dto.targetTableNm())
                .queryConditionCntt(dto.queryConditionCntt())
                .tableColumnListNm(dto.tableColumnListNm())
                .excelColumnListNm(dto.excelColumnListNm())
                .excelTableColumnListNm(dto.excelTableColumnListNm())
                .excelConditionCntt(dto.excelConditionCntt())
                .goodsDataSourceUnitNm(dto.goodsDataSourceUnitNm())
                .goodsDataSourceCurrencyUnitNm(dto.goodsDataSourceCurrencyUnitNm())
                .isCandleChartPossible(dto.isCandleChartPossible())
                .convertTargetDetailTypeNm(dto.convertTargetDetailTypeNm())
                .dataStructureConvertNm(dto.dataStructureConvertNm())
                .build();
    }
}