package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.dto.Audited;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tmd.custom.CustomCustomerCompanyRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Integer>, CustomCustomerCompanyRepository {

    @Query(value = """
            SELECT new com.sk.domain.shared.dto.Audited(
                cc,
                new com.sk.domain.shared.dto.AuditInfo(
                    cc.createUserId,
                    cc.createDt,
                    cc.modifyUserId,
                    cc.modifyDt,
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
            FROM CustomerCompany cc
              LEFT JOIN User creator ON creator.userLoginId = cc.createUserId AND creator.userRoleCd = 'ADMIN'
              LEFT JOIN creator.customerCompany creatorCompany
              LEFT JOIN User modifier ON modifier.userLoginId = cc.modifyUserId AND modifier.userRoleCd = 'ADMIN'
              LEFT JOIN modifier.customerCompany modifierCompany
            WHERE cc.isDeleted = false
              AND cc.customerCompanyNo = :customerCompanyNo
        """)
    Optional<Audited<CustomerCompany>> queryByCustomerCompanyNoAndIsDeletedFalseWithAuditor(@Param("customerCompanyNo") Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyNoAndIsDeletedFalse(Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyNoAndIsMipManagementCompanyTrueAndIsDeletedFalse(Integer customerCompanyNo);

    Optional<CustomerCompany> findByCustomerCompanyCdAndIsMipManagementCompanyFalseAndIsDeletedFalse(String customerCompanyCd);

    Optional<CustomerCompany> findByCustomerCompanyCdAndIsMipManagementCompanyTrueAndIsDeletedFalse(String customerCompanyCd);

    List<CustomerCompany> findByIsDeletedFalseOrderByCustomerCompanyNmAsc();

    List<CustomerCompany> findByIsDeletedFalseAndIsMipManagementCompanyFalseOrderByCustomerCompanyNmAsc();

    Long countByIsMipManagementCompanyTrueAndIsDeletedFalse();
}
