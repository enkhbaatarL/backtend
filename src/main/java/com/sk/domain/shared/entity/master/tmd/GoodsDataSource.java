package com.sk.domain.shared.entity.master.tmd;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
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
@Comment("TMD_상품데이터소스상세")
@Table(name = "tmd_gdsdatasrcd", indexes = {
        @Index(name = "TMD_GDSDATASRCD_IX01", columnList = "gds_no")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataSource extends BaseEntity {

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
    private String queryConditionContent;

    @Column(name = "gds_data_src_expn", length = 500)
    @Comment("상품데이터소스설명")
    private String goodsDataSourceExpn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_no", referencedColumnName = "gds_no", nullable = false)
    @Comment("상품번호")
    private Goods goods;

    @Builder
    public GoodsDataSource(Integer goodsDataSourceNo, String goodsDataSourceNm, String targetDbNm, String targetTableNm,
            String queryConditionContent, String goodsDataSourceExpn, Goods goods) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.targetDbNm = targetDbNm;
        this.targetTableNm = targetTableNm;
        this.queryConditionContent = queryConditionContent;
        this.goodsDataSourceExpn = goodsDataSourceExpn;
        this.goods = goods;
    }
}