package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsSignificance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NewsSignificanceRepository extends JpaRepository<NewsSignificance, Long>,
    JpaSpecificationExecutor<NewsSignificance> {
}
