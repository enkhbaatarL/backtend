package com.sk.domain.shared.entity.master.tlm;

import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import org.hibernate.annotations.Comment;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TLM_상품데이터조회사용자내역")
@Table(name = "tlm_gdsdatainqrusrl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataInquiryUser extends BaseEntity {

    @EmbeddedId
    private GoodsDataInquiryUserId id;

    public static GoodsDataInquiryUser of(GoodsDataInquiryUserId id) {
        GoodsDataInquiryUser goodsDataInquiryUser = new GoodsDataInquiryUser();
        goodsDataInquiryUser.id = id;
        return goodsDataInquiryUser;
    }

}
