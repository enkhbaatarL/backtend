package com.sk.domain.shared.entity.compositeKey.tam;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class CustomerCompanyProviderMappingId {

    @Column(name = "cust_cpny_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("고객회사번호")
    private Integer customerCompanyNo;

    @Column(name = "prvd_no", nullable = false, columnDefinition = "mediumint unsigned")
    @Comment("프로바이더번호")
    private Integer providerNo;

    @Builder
    public CustomerCompanyProviderMappingId(Integer customerCompanyNo, Integer providerNo) {
        this.customerCompanyNo = customerCompanyNo;
        this.providerNo = providerNo;
    }
}
