package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScreenElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreenElementRepository extends JpaRepository<ScreenElement, Integer> {

    List<ScreenElement> findByIsDeletedFalseOrderByDefaultOrderNoAscScreenElementNoAsc();

}
