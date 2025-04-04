package com.sk.domain.shared.entity.master.tmd;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Comment;

import com.sk.domain.admin.dto.request.provider.GoodsDataCreateReq;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
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
@Comment("TMD_상품데이터기본")
@Table(name = "tmd_gdsdatam")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gds_data_no", nullable = false, columnDefinition = "int unsigned")
    @Comment("상품데이터번호")
    private Integer goodsDataNo;

    @Column(name = "gds_data_dstg_id", length = 50)
    @Comment("상품데이터식별ID")
    private String goodsDataDistinguishId;

    @Column(name = "gds_data_nm", length = 100)
    @Comment("상품데이터명")
    private String goodsDataNm;

    @Column(name = "gds_data_expn", length = 500)
    @Comment("상품데이터설명")
    private String goodsDataExpn; // 사용 안 함

    @Column(name = "gds_data_smry_cntt", columnDefinition = "text")
    @Comment("상품데이터베이스명")
    private String goodsDataSummaryCntt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_no", referencedColumnName = "gds_no", nullable = false)
    @Comment("상품번호")
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @Builder
    public GoodsData(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer goodsDataNo, String goodsDataDistinguishId, String goodsDataNm, String goodsDataExpn, String goodsDataSummaryCntt, Goods goods,
            CustomerCompany customerCompany) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsDataNo = goodsDataNo;
        this.goodsDataDistinguishId = goodsDataDistinguishId;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataExpn = "";
        this.goodsDataSummaryCntt = goodsDataSummaryCntt;
        this.goods = goods;
        this.customerCompany = customerCompany;
    }

    public static GoodsData of(Goods goods, CustomerCompany customerCompany, GoodsDataCreateReq dto) {
        return GoodsData.builder()
                .goodsDataNm(dto.goodsDataNm())
                .goodsDataExpn("")
                .goodsDataSummaryCntt(dto.goodsDataSummaryCntt())
                .goods(goods)
                .customerCompany(customerCompany)
                .build();
    }

    public void update(String goodsDataSummaryCntt) {
        if (StringUtils.isNotEmpty(goodsDataSummaryCntt)) {
            this.goodsDataSummaryCntt = goodsDataSummaryCntt;
        }
    }

    public void softDelete() {
        super.softDelete();
    }
}