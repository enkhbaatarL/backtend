package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.dto.Audited;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyProviderMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.Provider;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyProviderMappingRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyProviderMappingRepository extends JpaRepository<CustomerCompanyProviderMapping, CustomerCompanyProviderMappingId>, CustomCustomerCompanyProviderMappingRepository {

    @EntityGraph(attributePaths = {"provider"})
    List<CustomerCompanyProviderMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany company);

    Optional<CustomerCompanyProviderMapping> findByCustomerCompanyAndProviderAndIsDeletedFalse(CustomerCompany company, Provider provider);

    @Query(value = """
            SELECT new com.sk.domain.shared.dto.Audited(
                ccpm,
                new com.sk.domain.shared.dto.AuditInfo(
                    ccpm.createUserId,
                    ccpm.createDt,
                    ccpm.modifyUserId,
                    ccpm.modifyDt,
                    new com.sk.domain.shared.dto.AuditExtension(
                        COALESCE(creator.userNm, null),
                        COALESCE(creator.departmentNm, null),
                        COALESCE(creatorCompany.customerCompanyNm, null),
                        COALESCE(modifier.userNm, null),
                        COALESCE(modifier.departmentNm, null),
                        COALESCE(modifierCompany.customerCompanyNm, null)
                    )
                )
            )
            FROM CustomerCompanyProviderMapping ccpm
              LEFT JOIN FETCH ccpm.provider prvd
              LEFT JOIN FETCH ccpm.customerCompany comp
              LEFT JOIN User creator ON creator.userLoginId = ccpm.createUserId AND creator.userRoleCd = 'ADMIN' and creator.isDeleted = false
              LEFT JOIN creator.customerCompany creatorCompany
              LEFT JOIN User modifier ON modifier.userLoginId = ccpm.modifyUserId AND modifier.userRoleCd = 'ADMIN' and modifier.isDeleted = false
              LEFT JOIN modifier.customerCompany modifierCompany
            WHERE ccpm.isDeleted = false
              AND ccpm.id.customerCompanyNo = :customerCompanyNo
              AND ccpm.provider.providerNo = :providerNo
        """)
    Optional<Audited<CustomerCompanyProviderMapping>> queryById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(Integer customerCompanyNo, Integer providerNo);

    Optional<CustomerCompanyProviderMapping> findById_CustomerCompanyNoAndId_ProviderNoAndIsDeletedFalse(Integer companyNo, Integer providerNo);

    @EntityGraph(attributePaths = {"provider"})
    List<CustomerCompanyProviderMapping> findById_CustomerCompanyNoAndIsDeletedFalse(Integer companyNo);

    Optional<CustomerCompanyProviderMapping> findByCustomerCompanyAndId_ProviderNoAndIsDeletedFalse(CustomerCompany company, Integer providerNo);

}
