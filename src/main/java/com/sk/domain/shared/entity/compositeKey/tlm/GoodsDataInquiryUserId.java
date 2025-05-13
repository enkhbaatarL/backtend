package com.sk.domain.shared.entity.compositeKey.tlm;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.master.tmd.GoodsData;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class GoodsDataInquiryUserId implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_data_no", referencedColumnName = "gds_data_no", nullable = false)
    @Comment("상품데이타번호")
    private GoodsData goodsData;

    @Column(name = "usr_logn_id", length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @Column(name = "inqr_dtm")
    @Comment("조회일시")
    private LocalDateTime inquiryDtm;

    @Builder
    public GoodsDataInquiryUserId(LocalDateTime inquiryDtm, String userLoginId, GoodsData goodsData) {
        this.inquiryDtm = inquiryDtm;
        this.userLoginId = userLoginId;
        this.goodsData = goodsData;
    }

    public static GoodsDataInquiryUserId of(GoodsData goodsData, String userLoginId) {
        return GoodsDataInquiryUserId.builder()
                .goodsData(goodsData)
                .userLoginId(userLoginId)
                .inquiryDtm(LocalDateTime.now())
                .build();
    }

}
