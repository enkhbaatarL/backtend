package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsRegionClassifications;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRegionClassificationsRepository extends
    JpaRepository<NewsRegionClassifications, Integer> {

    List<NewsRegionClassifications> findByNewsSummaryIdInAndIsDeletedFalse(
        List<Integer> newsSummaryIds);
}
