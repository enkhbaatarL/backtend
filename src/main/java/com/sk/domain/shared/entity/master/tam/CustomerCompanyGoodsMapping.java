package com.sk.domain.shared.entity.master.tam;

import java.time.LocalDateTime;

import com.sk.domain.admin.dto.request.company.CompanyGoodsMapReq;
import com.sk.domain.admin.dto.request.company.AuthGoodsMapReq;
import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import com.sk.domain.shared.listener.CustomerCompanyGoodsMappingListener;
import jakarta.persistence.*;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tam_custcpnygdsmappl", indexes = {
        @Index(name = "TAM_CUSTCPNYGDSMAPPL_FK02", columnList = "gds_no")
})
@Comment("TAM_고객회사상품매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(CustomerCompanyGoodsMappingListener.class)
public class CustomerCompanyGoodsMapping extends BaseEntity {

    @EmbeddedId
    private CustomerCompanyGoodsMappingId id;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @MapsId("customerCompanyNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false, referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @MapsId("goodsNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_no", nullable = false, referencedColumnName = "gds_no")
    @Comment("상품")
    private Goods goods;

    @Builder
    public CustomerCompanyGoodsMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
            String modifyUserId, Boolean isDeleted, CustomerCompanyGoodsMappingId id, Boolean isUsed,
            CustomerCompany customerCompany, Goods goods) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.isUsed = isUsed;
        this.customerCompany = customerCompany;
        this.goods = goods;
    }

    public static CustomerCompanyGoodsMapping of(CustomerCompany customerCompany, Goods goods, GoodsAuthManageSupportCd goodsAuthManageSupportCd) {
        return CustomerCompanyGoodsMapping.builder()
                .id(CustomerCompanyGoodsMappingId.of(customerCompany.getCustomerCompanyNo(), goods.getGoodsNo(), goodsAuthManageSupportCd))
                .isUsed(true)
                .customerCompany(customerCompany)
                .goods(goods)
                .build();
    }

    public static CustomerCompanyGoodsMapping of(CustomerCompany customerCompany, Goods goods, GoodsAuthManageSupportCd goodsAuthManageSupportCd, boolean isUsed) {
        return CustomerCompanyGoodsMapping.builder()
            .id(CustomerCompanyGoodsMappingId.of(customerCompany.getCustomerCompanyNo(), goods.getGoodsNo(), goodsAuthManageSupportCd))
            .isUsed(isUsed)
            .customerCompany(customerCompany)
            .goods(goods)
            .build();
    }

    public void update(AuthGoodsMapReq authGoodsMapReq) {

        if (ObjectUtils.isNotEmpty(authGoodsMapReq.isUsed())) {
            this.isUsed = authGoodsMapReq.isUsed();
        }

    }

}
