package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.repository.master.tmd.custom.CustomUnitConvertRepository;

public interface UnitConvertRepository extends JpaRepository<UnitConvert, Integer>, CustomUnitConvertRepository {

    List<UnitConvert> findByIsDeletedFalse();

    Optional<UnitConvert> findByUnitConvNoAndIsDeletedFalse(int unitConvNo);

    List<UnitConvert> findByConvertPreviousUnitNmAndIsDeletedFalse(String convertPreviousUnitNm);
}
