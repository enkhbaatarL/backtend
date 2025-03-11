package com.sk.domain.shared.entity.master.tam;

import org.hibernate.annotations.Comment;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Goods;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tam_custcpnygdsmappl", indexes = {
        @Index(name = "TAM_CUSTCPNYGDSMAPPL_FK02", columnList = "gds_no")
})
@Comment("TAM_고객회사상품매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerCompanyGoodsMapping extends BaseEntity {

    @EmbeddedId
    private CustomerCompanyGoodsMappingId id;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @MapsId("customerCompanyNo") // 복합 키의 customerCompanyNo와 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false, referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @MapsId("goodsNo") // 복합 키의 goodsNo와 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gds_no", nullable = false, referencedColumnName = "gds_no")
    @Comment("상품")
    private Goods goods;
}
