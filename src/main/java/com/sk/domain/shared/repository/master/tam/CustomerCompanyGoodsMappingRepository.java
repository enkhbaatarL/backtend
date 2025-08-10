package com.sk.domain.shared.repository.master.tam;

import com.sk.domain.shared.dto.Audited;
import com.sk.domain.shared.entity.compositeKey.tam.CustomerCompanyGoodsMappingId;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.repository.master.tam.custom.CustomCustomerCompanyGoodsMappingRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyGoodsMappingRepository
        extends JpaRepository<CustomerCompanyGoodsMapping, CustomerCompanyGoodsMappingId>, CustomCustomerCompanyGoodsMappingRepository {

    @EntityGraph(attributePaths = { "goods" })
    List<CustomerCompanyGoodsMapping> findByCustomerCompanyAndIsDeletedFalse(CustomerCompany customerCompany);

    @Query(value = """
            SELECT new com.sk.domain.shared.dto.Audited(
                ccgm,
                new com.sk.domain.shared.dto.AuditInfo(
                    ccgm.createUserId,
                    ccgm.createDt,
                    ccgm.modifyUserId,
                    ccgm.modifyDt,
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
            FROM CustomerCompanyGoodsMapping ccgm
              LEFT JOIN FETCH ccgm.goods goods
              LEFT JOIN User creator ON creator.userLoginId = ccgm.createUserId AND creator.userRoleCd = 'ADMIN' and creator.isDeleted = false
              LEFT JOIN creator.customerCompany creatorCompany
              LEFT JOIN User modifier ON modifier.userLoginId = ccgm.modifyUserId AND modifier.userRoleCd = 'ADMIN' and modifier.isDeleted = false
              LEFT JOIN modifier.customerCompany modifierCompany
            WHERE ccgm.isDeleted = false
              AND ccgm.id.customerCompanyNo = :customerCompanyNo
              AND ccgm.goods.goodsNo = :goodsNo
        """)
    Optional<Audited<CustomerCompanyGoodsMapping>> queryById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalseWithAuditor(Integer customerCompanyNo, Integer goodsNo);

    Optional<CustomerCompanyGoodsMapping> findById_CustomerCompanyNoAndId_GoodsNoAndIsDeletedFalse(Integer companyNo, Integer goodsNo);

    Optional<CustomerCompanyGoodsMapping> findByCustomerCompanyAndId_GoodsNoAndIsDeletedFalse(CustomerCompany company, Integer goodsNo);

}
