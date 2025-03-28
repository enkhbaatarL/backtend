package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitConvertRepository extends JpaRepository<UnitConvert, Integer> {

    List<UnitConvert> findByIsDeletedFalse();

    Optional<UnitConvert> findByUnitConvNoAndIsDeletedFalse(int unitConvNo);

}
