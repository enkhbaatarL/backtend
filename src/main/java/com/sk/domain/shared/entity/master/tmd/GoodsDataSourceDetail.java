package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.master.tcm.ComponentInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Comment("TMD_상품데이터소스상세")
@Table(name = "tmd_gdsdatasrcd")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataSourceDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_data_src_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("상품데이터소스번호")
    private Integer goodsDataSourceNo;

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

    @Column(name = "gds_data_src_expn", length = 500)
    @Comment("상품데이터소스설명")
    private String goodsDataSourceExpn;

    @Column(name = "gds_data_src_unit_nm", length = 50)
    @Comment("상품데이터소스단위명")
    private String goodsDataSourceUnitNm;

    @Column(name = "tbl_colm_list_nm", length = 255)
    @Comment("테이블컬럼목록명")
    private String tableColumnListNm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_no", referencedColumnName = "gds_data_no", nullable = false)
    @Comment("상품데이타번호")
    private GoodsData goodsData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmpnt_type_id", referencedColumnName = "cmpnt_type_id")
    private ComponentInfo componentInfo;

    @Column(name = "gds_data_src_rmk", nullable = false, length = 2000)
    @Comment("상품데이터소스비고")
    private String goodsDataSourceRmk;

    @Column(name = "gds_data_use_mnagt_cd", length = 10)
    @Comment("상품데이터사용주체코드")
    private String goodsDataUseManagementCd;

    @Builder
    public GoodsDataSourceDetail(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer goodsDataSourceNo, String goodsDataSourceNm, String targetDbNm, String targetTableNm,
            String queryConditionCntt, String goodsDataSourceExpn, String goodsDataSourceUnitNm, String tableColumnListNm, GoodsData goodsData) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionCntt = queryConditionCntt;
        this.goodsDataSourceExpn = goodsDataSourceExpn;
        this.goodsDataSourceUnitNm = goodsDataSourceUnitNm;
        this.tableColumnListNm = tableColumnListNm;
        this.goodsData = goodsData;
    }

    public static GoodsDataSourceDetail of(GoodsData goodsData, String goodsDataSourceNm, String targetDbNm, String targetTableNm,
            String queryConditionCntt, String goodsDataSourceExpn, String goodsDataSourceUnitNm, String tableColumnListNm) {
        return GoodsDataSourceDetail.builder()
                .goodsDataSourceNm(goodsDataSourceNm)
                .targetDbNm(targetDbNm)
                .targetTableNm(targetTableNm)
                .queryConditionCntt(queryConditionCntt)
                .goodsDataSourceExpn(goodsDataSourceExpn)
                .goodsDataSourceUnitNm(goodsDataSourceUnitNm)
                .tableColumnListNm(tableColumnListNm)
                .goodsData(goodsData)
                .build();
    }

    public void softDelete() {
        super.softDelete();
    }
}