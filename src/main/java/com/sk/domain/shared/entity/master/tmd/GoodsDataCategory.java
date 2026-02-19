package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

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
@Comment("TMD_상품데이터카테고리내역")
@Table(name = "tmd_gdsdatactgrl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_ctgr_reg_no", nullable = false, columnDefinition = "bigint unsigned")
    @Comment("상품카테고리등록번호")
    private Long goodsCategoryRegistrationNo;

    @Column(name = "gds_data_ctgr_cd", nullable = false, length = 3)
    @Comment("상품데이터카테고리코드")
    private String goodsDataCategoryCd;

    @Column(name = "gds_data_keywd_nm", nullable = false, length = 100)
    @Comment("상품데이터키워드명")
    private String goodsDataKeywordNm;

    @Column(name = "gds_data_mnagt_spt_cd", nullable = false, length = 5)
    @Comment("상품데이터주체구분코드")
    private String goodsDataManagementSeparateCd;

    @Column(name = "gds_data_mnagt_no")
    @Comment("상품데이터주체번호")
    private Long goodsDataManagementNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_no", referencedColumnName = "gds_data_no", nullable = false)
    @Comment("상품데이타번호")
    private GoodsData goodsData;

    @Builder
    public GoodsDataCategory(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Long goodsCategoryRegistrationNo, String goodsDataCategoryCd, String goodsDataKeywordNm, String goodsDataManagementSeparateCd,
            Long goodsDataManagementNo, GoodsData goodsData) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsCategoryRegistrationNo = goodsCategoryRegistrationNo;
        this.goodsDataCategoryCd = goodsDataCategoryCd;
        this.goodsDataKeywordNm = goodsDataKeywordNm;
        this.goodsDataManagementSeparateCd = goodsDataManagementSeparateCd;
        this.goodsDataManagementNo = goodsDataManagementNo;
        this.goodsData = goodsData;
    }

    public static GoodsDataCategory of(GoodsData goodsData, String goodsDataCategoryCd, String goodsDataKeywordNm,
            String goodsDataManagementSeparateCd, Long goodsDataManagementNo) {
        return GoodsDataCategory.builder()
                .goodsDataCategoryCd(goodsDataCategoryCd)
                .goodsDataKeywordNm(goodsDataKeywordNm)
                .goodsDataManagementSeparateCd(goodsDataManagementSeparateCd)
                .goodsData(goodsData)
                .goodsDataManagementNo(goodsDataManagementNo)
                .build();
    }

    public void softDelete() {
        super.softDelete();
    }

    public void updateKeywordNm(String goodsDataKeywordNm) {
        this.goodsDataKeywordNm = goodsDataKeywordNm;
        this.modifyDt = LocalDateTime.now();
    }
}
