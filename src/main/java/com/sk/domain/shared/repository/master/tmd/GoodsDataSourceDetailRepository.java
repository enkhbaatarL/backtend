package com.sk.domain.shared.repository.master.tmd;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

public interface GoodsDataSourceDetailRepository extends JpaRepository<GoodsDataSourceDetail, Integer> {
     Optional<GoodsDataSourceDetail> findByGoodsDataSourceNoAndIsDeletedFalse(Integer goodsDataSourceNo);
}
