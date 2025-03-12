package com.sk.domain.shared.entity.master.tam;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyProviderMappingId;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tam_custcpnyprvdmappl", indexes = {
        @Index(name = "TAM_CUSTCPNYPRVDMAPPL_FK02", columnList = "prvd_no")
})
@Comment("TAM_고객회사프로바이더매핑내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
    public CustomerCompanyProviderMapping(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
            String modifyUserId, Boolean isDeleted, CustomerCompanyProviderMappingId id,
            CustomerCompany customerCompany, Provider provider) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.customerCompany = customerCompany;
        this.provider = provider;
    }
}
