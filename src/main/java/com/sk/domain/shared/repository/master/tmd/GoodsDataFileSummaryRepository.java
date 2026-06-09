package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataFileSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsDataFileSummaryRepository extends JpaRepository<GoodsDataFileSummary, Integer> {
    List<GoodsDataFileSummary> findByGoodsDataAndIsDeletedFalse(GoodsData goodsData);

    Optional<GoodsDataFileSummary> findFirstByGoodsDataGoodsDataNoAndIsDeletedFalseOrderByCreateDtDesc(Integer goodsDataNo);
}
