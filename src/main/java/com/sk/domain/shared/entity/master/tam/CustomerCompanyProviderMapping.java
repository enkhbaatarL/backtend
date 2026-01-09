package com.sk.domain.shared.entity.master.tam;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.request.company.CompanyProviderMapReq;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyProviderMappingId;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.listener.CustomerCompanyProviderMappingListener;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tam_custcpnyprvdmappl")
@Comment("TAM_고객회사프로바이더매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(CustomerCompanyProviderMappingListener.class)
public class CustomerCompanyProviderMapping extends BaseEntity {

    @EmbeddedId
    private CustomerCompanyProviderMappingId id;

    @MapsId("customerCompanyNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_cpny_no", nullable = false, referencedColumnName = "cust_cpny_no")
    @Comment("고객회사")
    private CustomerCompany customerCompany;

    @MapsId("providerNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prvd_no", nullable = false, referencedColumnName = "prvd_no")
    @Comment("프로바이더")
    private Provider provider;

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1)")
    @Comment("사용여부")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isUsed;

    @Column(name = "defl_ustrc_yn", nullable = false, length = 1, columnDefinition = "char(1) DEFAULT 'N'")
    @Comment("기본설정여부(비정형)")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isDefaultUnstructured;

    @Builder
    public CustomerCompanyProviderMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId, Boolean isDeleted, CustomerCompanyProviderMappingId id, CustomerCompany customerCompany, Provider provider, Boolean isUsed) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.customerCompany = customerCompany;
        this.provider = provider;
        this.isUsed = isUsed;
    }

    public static CustomerCompanyProviderMapping of(CustomerCompany company, Provider provider) {
        return CustomerCompanyProviderMapping.builder()
            .id(CustomerCompanyProviderMappingId.of(company.getCustomerCompanyNo(), provider.getProviderNo()))
            .customerCompany(company)
            .provider(provider)
            .isUsed(Boolean.TRUE)
            .build();
    }

    public void update(CompanyProviderMapReq.Update companyProviderMapReq) {

        if (ObjectUtils.isNotEmpty(companyProviderMapReq.isUsed())) {
            this.isUsed = companyProviderMapReq.isUsed();
        }

    }

}
