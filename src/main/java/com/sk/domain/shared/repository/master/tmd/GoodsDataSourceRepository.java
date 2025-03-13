package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.GoodsDataSourceRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSource;

public interface GoodsDataSourceRepository extends JpaRepository<GoodsDataSource, Integer> {

  @Query(value = """
      select new com.sk.domain.admin.dto.response.GoodsDataSourceRes$All(
      gds.goodsDataSourceNo,
      gds.goodsDataSourceNm,
      gds.targetDbNm,
      gds.targetTableNm,
      gds.queryConditionContent,
      gds.goodsDataSourceExpn,
      gds.createUserId,
      gds.createDt,
      gds.modifyUserId,
      gds.modifyDt
        )
      from GoodsDataSource gds
      where gds.goods.goodsNo = :goodsNo
      and (gds.goodsDataSourceNm like concat('%', :keyword, '%') or gds.goodsDataSourceExpn like concat('%', :keyword, '%'))
            """)
  Page<GoodsDataSourceRes.All> findByGoodsNoAndIsDeletedFalse(@Param("goodsNo") int goodsNo,
      @Param("keyword") String keyword, Pageable pageable);
}
