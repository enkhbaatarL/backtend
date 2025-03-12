package com.sk.domain.shared.entity.compositeKey.tam;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Comment;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class CustomerCompanyGoodsMappingId implements Serializable {

    @Column(name = "cust_cpny_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("고객회사번호")
    private Integer customerCompanyNo;
    
    @Column(name = "gds_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("상품번호")
    private Integer goodsNo;

    @Column(name = "gds_auth_mnagt_spt_cd", nullable = false, length = 10)
    @Comment("상품권한주체구분코드")
    private String goodsAuthManageSupportCd;
}
