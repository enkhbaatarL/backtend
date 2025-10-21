package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NewsSummaryRepository extends JpaRepository<NewsSummary, Integer>,
    JpaSpecificationExecutor<NewsSummary> {

}
