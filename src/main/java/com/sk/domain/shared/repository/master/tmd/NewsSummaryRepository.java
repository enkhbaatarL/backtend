package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Integer> {

    Page<NewsSummary> findAllByOrderByFirstCreatedDescDataRegistrationSnDesc(Pageable pageable);

}
