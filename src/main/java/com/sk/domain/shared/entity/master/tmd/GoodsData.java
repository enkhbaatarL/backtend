package com.sk.domain.shared.entity.master.tmd;

import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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

    @Column(name = "gds_data_nm", length = 500)
    @Comment("상품데이터명")
    private String goodsDataNm;

    @Column(name = "gds_data_expn", columnDefinition = "text")
    @Comment("상품데이터설명")
    private String goodsDataExpn;

    @Column(name = "gds_data_smry_cntt", columnDefinition = "text")
    @Comment("상품데이터요약내용")
    private String goodsDataSummaryCntt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_no", referencedColumnName = "gds_no", nullable = false)
    @Comment("상품번호")
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @Column(name = "data_ctrb_nm", length = 50)
    @Comment("데이터기고자명")
    private String dataContributorNm;

    @Column(name = "metad_upld_yn", length = 1, columnDefinition = "char(1)")
    @Comment("메타데이타업로드여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isMetaDataUploaded;

    @Column(name = "gds_data_mark_yn", length = 1, columnDefinition = "char(1)")
    @Comment("상품데이타표시여부")
    @ColumnDefault("'Y'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isGoodsDataMarked;

    @Column(name = "metad_upld_file_nm", length = 300)
    @Comment("metad_upld_file_nm")
    private String metadUpldFileNm;

    @Column(name = "pub_dt")
    @Comment("발행일")
    private LocalDate publishedDate;

    @Column(name = "authors", length = 200)
    @Comment("저자 리스트")
    private String authors;

    @Builder
    public GoodsData(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted,
            Integer goodsDataNo, String goodsDataDistinguishId, String goodsDataNm, String goodsDataExpn, String goodsDataSummaryCntt, Goods goods,
            CustomerCompany customerCompany, String dataContributorNm, Boolean isMetaDataUploaded, Boolean isGoodsDataMarked, LocalDate publishedDate, String authors) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.goodsDataNo = goodsDataNo;
        this.goodsDataDistinguishId = goodsDataDistinguishId;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataExpn = goodsDataExpn;
        this.goodsDataSummaryCntt = goodsDataSummaryCntt;
        this.goods = goods;
        this.customerCompany = customerCompany;
        this.dataContributorNm = dataContributorNm;
        this.isMetaDataUploaded = isMetaDataUploaded;
        this.isGoodsDataMarked = isGoodsDataMarked;
        this.publishedDate = publishedDate;
        this.authors = authors;
    }

    public static GoodsData of(Goods goods, CustomerCompany customerCompany, String goodsDataNm, String goodsDataExpn, String goodsDataSummaryCntt,
            String dataContributorNm, Boolean isMetaDataUploaded, Boolean isGoodsDataMarked, LocalDate publishedDate, String authors) {
        return GoodsData.builder()
                .goodsDataNm(goodsDataNm)
                .goodsDataExpn(goodsDataExpn)
                .goodsDataSummaryCntt(goodsDataSummaryCntt)
                .goods(goods)
                .customerCompany(customerCompany)
                .dataContributorNm(dataContributorNm)
                .isMetaDataUploaded(isMetaDataUploaded)
                .isGoodsDataMarked(isGoodsDataMarked)
                .publishedDate(publishedDate)
                .authors(authors)
                .build();
    }

    public void update(String goodsDataExpn, String goodsDataSummaryCntt, String dataContributorNm, LocalDate publishedDate, String authors) {
        if (StringUtils.isNotEmpty(goodsDataExpn)) {
            this.goodsDataExpn = goodsDataExpn;
        }

        if (StringUtils.isNotEmpty(goodsDataSummaryCntt)) {
            this.goodsDataSummaryCntt = goodsDataSummaryCntt;
        }

        if (StringUtils.isNotEmpty(dataContributorNm)) {
            this.dataContributorNm = dataContributorNm;
        }
        if (publishedDate != null) {
            this.publishedDate = publishedDate;
        }
        if (StringUtils.isNotEmpty(authors)) {
            this.authors = authors;
        }

        this.modifyDt = LocalDateTime.now();
    }

    public void updateGoodsDataDistinguishId(String goodsDataDistinguishId) {
        this.goodsDataDistinguishId = goodsDataDistinguishId;
    }

    public void updateMetaDataUploadStatus(Boolean isMetaDataUploaded) {
        this.isMetaDataUploaded = isMetaDataUploaded;
    }

    public void updateModifyDt() {
        this.modifyDt = LocalDateTime.now();
    }

    public void updateMetadUpldFileNm(String metadUpldFileNm) {
        this.metadUpldFileNm = metadUpldFileNm;
    }

    public void softDelete() {
        super.softDelete();
    }

    public List<String> getAuthors(){
        if(this.authors == null || this.authors.trim().isEmpty()){
            return List.of("");
        }
        return Arrays.stream(this.authors.trim().split(",")).map(String::trim).toList();
    }

    public String getStringOfAuthors(){
        return this.authors;
    }
}
