package com.sk.domain.shared.repository.master.tmd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSource;

public interface GoodsDataSourceRepository extends JpaRepository<GoodsDataSource, Integer> {

  @Query(value = """
      select new com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes(
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
      where gds.isDeleted = false
      and gds.goods.provider.providerNo = :providerNo
      and gds.goods.goodsNo = :goodsNo
      and (
      lower(gds.goodsDataSourceNm) like concat('%', lower(:keyword), '%') or
      lower(gds.goodsDataSourceExpn) like concat('%', lower(:keyword), '%')
      )
      order by gds.goodsDataSourceNm asc, gds.goodsDataSourceExpn asc
            """, countQuery = """
      select count(gds)
      from GoodsDataSource gds
      where gds.isDeleted = false
      and gds.goods.provider.providerNo = :providerNo
      and gds.goods.goodsNo = :goodsNo
      and (
      lower(gds.goodsDataSourceNm) like concat('%', lower(:keyword), '%') or
      lower(gds.goodsDataSourceExpn) like concat('%', lower(:keyword), '%')
      )
      """)
  Page<GoodsDataSourceRes> findByProviderNoAndGoodsNoAndIsDeletedFalse(@Param("providerNo") Integer providerNo, @Param("goodsNo") Integer goodsNo,
      @Param("keyword") String keyword, Pageable pageable);
}
