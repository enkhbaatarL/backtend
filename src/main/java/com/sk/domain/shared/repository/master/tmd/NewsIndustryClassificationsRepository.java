package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.NewsIndustryClassifications;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsIndustryClassificationsRepository extends
    JpaRepository<NewsIndustryClassifications, Integer> {

    List<NewsIndustryClassifications> findByNewsSummaryIdInAndIsDeletedFalse(
        List<Integer> newsSummaryIds);

}
