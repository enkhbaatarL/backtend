package com.sk.domain.shared.entity.master.tlm;

import org.hibernate.annotations.Comment;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInqueryUserId;
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
public class GoodsDataInqueryUser extends BaseEntity {

    @EmbeddedId
    private GoodsDataInqueryUserId id;

    public static GoodsDataInqueryUser of(GoodsDataInqueryUserId id) {
        GoodsDataInqueryUser goodsDataInqueryUser = new GoodsDataInqueryUser();
        goodsDataInqueryUser.id = id;
        return goodsDataInqueryUser;
    }
}
