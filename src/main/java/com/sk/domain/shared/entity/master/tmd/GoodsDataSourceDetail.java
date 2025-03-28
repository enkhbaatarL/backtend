package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TMD_상품데이터소스상세")
@Table(name = "tmd_gdsdatasrcd", indexes = {
        @Index(name = "TMD_GDSDATASRCD_IX01", columnList = "gds_data_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataSourceDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_data_src_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("상품데이터소스번호")
    private Integer goodsDataSourceNo;

    @Column(name = "gds_data_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("상품데이터번호")
    private Integer goodsDataNo;

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

    @Builder
    public GoodsDataSourceDetail(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer goodsDataSourceNo, Integer goodsDataNo, String goodsDataSourceNm, String targetDbNm, String targetTableNm,
            String queryConditionCntt, String goodsDataSourceExpn, String goodsDataSourceUnitNm) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionCntt = queryConditionCntt;
        this.goodsDataSourceExpn = goodsDataSourceExpn;
        this.goodsDataSourceUnitNm = goodsDataSourceUnitNm;
    }

    public void softDelete() {
        super.softDelete();
    }
}