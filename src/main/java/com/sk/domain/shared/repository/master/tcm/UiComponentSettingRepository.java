package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.UiComponentSetting;
import java.util.List;
import java.util.Optional;
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

    @Query(
        """
            select u
            from UiComponentSetting u
            where u.customerCompany.customerCompanyNo = :companyNo
              and u.screenCode = :screenCode
              and u.componentTypeCode = :componentTypeCode
              and u.componentCode = :componentCode
              and u.isDeleted = false
            order by case when u.orderNo is null then 1 else 0 end, u.orderNo asc
            """
    )
    List<UiComponentSetting> findOrdered(
        @Param("companyNo") Integer companyNo,
        @Param("screenCode") String screenCode,
        @Param("componentTypeCode") String componentTypeCode,
        @Param("componentCode") String componentCode
    );

    @Query(
        """
            select u
            from UiComponentSetting u
            where u.customerCompany.customerCompanyNo = :companyNo
              and u.isDeleted = false
              and (:screenCode is null or u.screenCode = :screenCode)
              and (:componentTypeCode is null or u.componentTypeCode = :componentTypeCode)
              and (:componentCode is null or u.componentCode = :componentCode)
            order by case when u.orderNo is null then 1 else 0 end, u.orderNo asc
            """
    )
    List<UiComponentSetting> findByCompanyAndOptionalFilters(
        @Param("companyNo") Integer companyNo,
        @Param("screenCode") String screenCode,
        @Param("componentTypeCode") String componentTypeCode,
        @Param("componentCode") String componentCode
    );

    Optional<UiComponentSetting>
        findFirstByCustomerCompany_CustomerCompanyNoAndScreenCodeAndComponentTypeCodeAndComponentCodeAndItemKeyAndIsDeletedFalse(
            Integer companyNo,
            String screenCode,
            String componentTypeCode,
            String componentCode,
            String itemKey
        );
}
