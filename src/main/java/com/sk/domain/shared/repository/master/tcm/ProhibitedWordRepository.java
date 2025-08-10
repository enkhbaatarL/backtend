package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.ProhibitedWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProhibitedWordRepository extends JpaRepository<ProhibitedWord, Long> {

    List<ProhibitedWord> findByIsUsedTrueAndIsDeletedFalse();

}
