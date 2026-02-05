package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UiComponentSettingRepository
    extends JpaRepository<UiComponentSetting, Integer> {

    List<UiComponentSetting>
        findByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndIsDeletedFalse(
            Integer companyNo,
            String screenCode,
            String componentTypeCode,
            String componentCode
        );

    @Query(
        """
            select u
            from UiComponentSetting u
            where u.customerCompany.customerCompanyNo = :companyNo
              and u.screenCode = :screenCode
              and u.componentTypeCode = :componentTypeCode
              and u.componentCode = :componentCode
              and u.isDeleted = false
              and u.isVisible = true
            order by case when u.orderNo is null then 1 else 0 end, u.orderNo asc
            """
    )
    List<UiComponentSetting> findVisibleOrdered(
        @Param("companyNo") Integer companyNo,
        @Param("screenCode") String screenCode,
        @Param("componentTypeCode") String componentTypeCode,
        @Param("componentCode") String componentCode
    );
}
