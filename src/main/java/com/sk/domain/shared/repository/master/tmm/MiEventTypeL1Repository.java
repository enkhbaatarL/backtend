package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiEventTypeL1;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiEventTypeL1Repository extends JpaRepository<MiEventTypeL1, Long> {

    List<MiEventTypeL1> findAllByIsDeletedFalseOrderByEventTypeL1IdAsc();

}
