package com.sk.domain.shared.repository.master.tcm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import java.util.Optional;

public interface ChartPredictSettingRepository extends JpaRepository<ChartPredictSetting, Integer> {

    boolean existsByCustomerCompany(CustomerCompany customerCompany);

    @Query(value = """
            SELECT cps FROM ChartPredictSetting cps
            WHERE cps.customerCompany.customerCompanyNo = :companyNo
                AND cps.customerCompany.isDeleted = false
            """)
    Optional<ChartPredictSetting> findByCustomerCompanyNo(@Param("companyNo") Integer companyNo);

    Optional<ChartPredictSetting> findByCustomerCompany_CustomerCompanyNoAndIsDeletedFalse(Integer companyNo);

}
